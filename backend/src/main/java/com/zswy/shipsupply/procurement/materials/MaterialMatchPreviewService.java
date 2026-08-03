package com.zswy.shipsupply.procurement.materials;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.auth.TokenService;
import com.zswy.shipsupply.common.material.MaterialNameNormalizer;
import com.zswy.shipsupply.common.material.NormalizedMaterialName;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemRepository;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemResponse;

@Service
public class MaterialMatchPreviewService {

    static final String DEMAND_INQUIRY = "DEMAND_INQUIRY";
    static final String SUPPLIER_QUOTATION = "SUPPLIER_QUOTATION";
    static final String UNKNOWN = "UNKNOWN";

    private static final int MAX_CANDIDATES = 5;
    private static final int MAX_SUPPLIER_CANDIDATES = 10;
    private static final double FULL_MATCH_NAME_THRESHOLD = 0.6;
    private static final double FULL_MATCH_SPEC_THRESHOLD = 0.5;
    private static final String REASON_CODE_MATCH = "CODE_MATCH";
    private static final String REASON_NAME_SPEC_MATCH = "NAME_SPEC_MATCH";
    private static final String REASON_NAME_MATCH_SPEC_CHECK = "NAME_MATCH_SPEC_CHECK";
    private static final String REASON_NAME_MATCH_LOW_CONFIDENCE = "NAME_MATCH_LOW_CONFIDENCE";
    private static final String REASON_NAME_SPEC_MISMATCH = "NAME_SPEC_MISMATCH";
    private static final Pattern STANDARD_IMPA_CODE = Pattern.compile("\\d{6}");
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^A-Za-z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of(
        "AND", "THE", "FOR", "WITH", "WITHOUT", "OF", "IN", "ON", "A", "AN",
        "NO", "TYPE", "PCS", "PC", "SET", "BOX", "CTN", "INNER", "OUTER",
        "CM", "MM", "KG", "KGS", "RMB", "FOB", "WAREHOUSE", "MODEL", "SIZE",
        "WHITE", "BLACK", "BLUE", "RED", "GREEN", "YELLOW", "BROWN", "GREY", "GRAY",
        "COLOR", "COLOUR"
    );

    private final TokenService tokenService;
    private final AuthRepository authRepository;
    private final ImpaItemRepository impaItemRepository;
    private final XlsxMaterialQuoteParser parser;
    private final MaterialSupplierCandidateProvider supplierCandidateProvider;
    private final MaterialNameNormalizer normalizer;

    @Autowired
    public MaterialMatchPreviewService(
        TokenService tokenService,
        AuthRepository authRepository,
        ImpaItemRepository impaItemRepository,
        XlsxMaterialQuoteParser parser,
        MaterialSupplierCandidateProvider supplierCandidateProvider
    ) {
        this(tokenService, authRepository, impaItemRepository, parser, supplierCandidateProvider, new MaterialNameNormalizer());
    }

    MaterialMatchPreviewService(
        TokenService tokenService,
        ImpaItemRepository impaItemRepository,
        XlsxMaterialQuoteParser parser,
        MaterialSupplierCandidateProvider supplierCandidateProvider
    ) {
        this(tokenService, null, impaItemRepository, parser, supplierCandidateProvider, new MaterialNameNormalizer());
    }

    MaterialMatchPreviewService(
        TokenService tokenService,
        AuthRepository authRepository,
        ImpaItemRepository impaItemRepository,
        XlsxMaterialQuoteParser parser,
        MaterialSupplierCandidateProvider supplierCandidateProvider,
        MaterialNameNormalizer normalizer
    ) {
        this.tokenService = tokenService;
        this.authRepository = authRepository;
        this.impaItemRepository = impaItemRepository;
        this.parser = parser;
        this.supplierCandidateProvider = supplierCandidateProvider;
        this.normalizer = normalizer;
    }

    public MaterialMatchPreviewResponse matchPreview(String authorizationHeader, MultipartFile file) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .xlsx files are supported");
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("material-match-preview-", ".xlsx");
            file.transferTo(tempFile);
            String sourceFileId = saveTemplateFile(userId, file, tempFile);
            return matchDocument(parser.parse(tempFile), sourceFileId, originalFilename(file));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse uploaded file", ex);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Temporary upload cleanup is best-effort; no business data is written.
                }
            }
        }
    }

    MaterialMatchPreviewResponse matchRows(List<MaterialQuoteRow> rows) {
        String documentType = rows.isEmpty() ? UNKNOWN : rows.get(0).documentType();
        String sourceFormat = rows.isEmpty() ? XlsxMaterialQuoteParser.SOURCE_FORMAT_UNKNOWN : rows.get(0).sourceFormat();
        int headerRowIndex = rows.isEmpty() ? 0 : rows.get(0).headerRowIndex();
        return matchDocument(new MaterialParsedDocument(documentType, sourceFormat, headerRowIndex, rows));
    }

    MaterialMatchPreviewResponse matchDocument(MaterialParsedDocument document) {
        return matchDocument(document, null, null);
    }

    MaterialMatchPreviewResponse matchDocument(MaterialParsedDocument document, String sourceFileId, String sourceFileName) {
        SearchCache searchCache = new SearchCache();
        List<MaterialMatchPreviewItem> items = document.rows().stream()
            .map(row -> matchRow(row, searchCache))
            .toList();
        long exactCount = items.stream().filter(item -> "EXACT".equals(item.matchResult())).count();
        long similarCount = items.stream().filter(item -> "SIMILAR".equals(item.matchResult())).count();
        long unmatchedCount = items.stream().filter(item -> "UNMATCHED".equals(item.matchResult())).count();
        return new MaterialMatchPreviewResponse(
            document.documentType(),
            document.sourceFormat(),
            sourceFileId,
            sourceFileName,
            document.headerContext().inquiryNo(),
            document.headerContext().requestNo(),
            document.headerContext().vesselName(),
            document.headerContext().materialType(),
            document.headerContext().currency(),
            document.headerContext().suggestedPort(),
            document.headerContext().eta(),
            document.headerContext().recipientCompany(),
            document.headerContext().handlerName(),
            document.headerContext().handlerEmail(),
            document.headerContext().rawHeaderFields(),
            document.headerRowIndex(),
            items.size(),
            Math.toIntExact(exactCount),
            Math.toIntExact(similarCount),
            Math.toIntExact(unmatchedCount),
            items
        );
    }

    private String saveTemplateFile(Long userId, MultipartFile file, Path source) throws IOException {
        if (authRepository == null) {
            return null;
        }
        Path uploadDir = Path.of("uploads", "material-templates").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        String originalName = originalFilename(file);
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path target = Files.createTempFile(uploadDir, "material-template-", "-" + safeName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return authRepository.insertFile(
            userId,
            originalName,
            target.toString(),
            file.getContentType() == null || file.getContentType().isBlank()
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : file.getContentType(),
            Files.size(target)
        );
    }

    private String originalFilename(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        return originalName == null || originalName.isBlank() ? "material-template.xlsx" : originalName;
    }

    private MaterialMatchPreviewItem matchRow(
        MaterialQuoteRow row,
        SearchCache searchCache
    ) {
        NormalizedMaterialName normalized = normalizeRow(row);
        MatchDecision decision = switch (row.documentType()) {
            case DEMAND_INQUIRY -> matchDemandInquiry(row, normalized, searchCache);
            case SUPPLIER_QUOTATION -> matchSupplierQuotation(row, searchCache);
            default -> matchUnknown(row, searchCache);
        };
        MaterialMatchCandidate best = decision.candidates().isEmpty() ? null : decision.candidates().get(0);
        List<String> riskFlags = decision.riskFlags().isEmpty() ? normalized.riskFlags() : decision.riskFlags();
        ValidationDecision validation = validationDecision(row, decision);
        return new MaterialMatchPreviewItem(
            row.documentType(),
            row.sourceFormat(),
            row.headerRowIndex(),
            row.sequence(),
            row.sourceRowNo(),
            row.sourceRowNo(),
            row.rawColumns(),
            normalized.cleanName(),
            normalized.coreName(),
            normalized.attributes(),
            riskFlags,
            row.impaCode(),
            row.description(),
            row.sizeModel(),
            row.quantity(),
            row.unit(),
            row.remarks(),
            row.supplierItemNo(),
            row.rawNameSpec(),
            DEMAND_INQUIRY.equals(row.documentType()) ? null : row.price(),
            row.packing(),
            row.stock(),
            best == null ? null : best.impaCode(),
            best == null ? null : best.nameCn(),
            best == null ? null : best.nameEn(),
            best == null ? null : best.specification(),
            decision.matchResult(),
            decision.matchResultName(),
            decision.reason(),
            validation.status(),
            validation.reason(),
            row.hasImage(),
            row.imageIndex(),
            row.imageAnchor(),
            decision.candidates(),
            List.of()
        );
    }

    private ValidationDecision validationDecision(MaterialQuoteRow row, MatchDecision decision) {
        if (REASON_CODE_MATCH.equals(decision.reason()) && "EXACT".equals(decision.matchResult())) {
            return new ValidationDecision("MATCHED", "CODE_MATCH");
        }
        String code = normalizeCode(row.impaCode());
        if (code.isBlank()) {
            return new ValidationDecision("ABNORMAL", "CODE_MISSING");
        }
        if (!STANDARD_IMPA_CODE.matcher(code).matches()) {
            return new ValidationDecision("ABNORMAL", "CODE_FORMAT_INVALID");
        }
        return new ValidationDecision("ABNORMAL", "CODE_NOT_FOUND");
    }

    private NormalizedMaterialName normalizeRow(MaterialQuoteRow row) {
        if (DEMAND_INQUIRY.equals(row.documentType())) {
            return normalizer.normalize(row.description(), row.sizeModel(), row.packing());
        }
        if (SUPPLIER_QUOTATION.equals(row.documentType())) {
            return normalizer.normalize(row.rawNameSpec(), null, row.packing());
        }
        return normalizer.normalize(String.join(" ", row.rawColumns().values()), null, null);
    }

    private List<MaterialSupplierCandidate> supplierCandidates(
        MaterialQuoteRow row,
        MaterialMatchCandidate best,
        List<MaterialSupplierCandidate> supplierPool
    ) {
        List<String> nameTokens = mergeTokens(tokenize(row.description()), tokenize(row.rawNameSpec()));
        List<String> specTokens = mergeTokens(tokenize(row.sizeModel()), tokenize(row.packing()));
        String supplierSku = normalizeCode(row.supplierItemNo());
        Set<String> platformCodes = new LinkedHashSet<>();
        addCode(platformCodes, row.impaCode());
        if (best != null) {
            addCode(platformCodes, best.impaCode());
        }
        String preferredCategory = best == null ? null : best.categoryCode();
        BigDecimal requestedQty = parseDecimal(row.quantity());

        return supplierPool.stream()
            .filter(candidate -> "ON_SHELF".equalsIgnoreCase(nullToEmpty(candidate.shelfStatus())))
            .map(candidate -> scoreSupplierCandidate(candidate, supplierSku, platformCodes, nameTokens, specTokens, preferredCategory, requestedQty))
            .filter(scored -> scored.rank() > 0)
            .sorted(supplierCandidateComparator())
            .limit(MAX_SUPPLIER_CANDIDATES)
            .map(SupplierScoredCandidate::candidate)
            .toList();
    }

    private SupplierScoredCandidate scoreSupplierCandidate(
        MaterialSupplierCandidate candidate,
        String supplierSku,
        Set<String> platformCodes,
        List<String> nameTokens,
        List<String> specTokens,
        String preferredCategory,
        BigDecimal requestedQty
    ) {
        boolean codeMatch = platformCodes.stream()
            .anyMatch(code -> code.equals(normalizeCode(candidate.impaCode())) || code.equals(normalizeCode(candidate.platformCode())));
        boolean supplierSkuMatch = !supplierSku.isBlank() && supplierSku.equals(normalizeCode(candidate.supplierSkuCode()));
        double nameScore = coverage(nameTokens, tokenize(candidate.productName()));
        double specScore = coverage(specTokens, mergeTokens(tokenize(candidate.attributeSummary()), tokenize(candidate.packageSpec())));
        boolean nameSpecMatch = nameScore >= FULL_MATCH_NAME_THRESHOLD
            || (!nameTokens.isEmpty() && nameScore > 0 && (specTokens.isEmpty() || specScore > 0));
        boolean categoryMatch = preferredCategory != null && preferredCategory.equals(candidate.categoryCode());

        String matchType = null;
        String reason = null;
        int rank = 0;
        if (codeMatch) {
            rank = 4;
            matchType = "CODE_EXACT";
            reason = "IMPA_OR_PLATFORM_CODE_MATCH";
        } else if (supplierSkuMatch) {
            rank = 3;
            matchType = "SUPPLIER_SKU_MATCH";
            reason = "SUPPLIER_SKU_MATCH";
        } else if (nameSpecMatch) {
            rank = 2;
            matchType = "NAME_SPEC_MATCH";
            reason = specTokens.isEmpty() || specScore > 0 ? "NAME_SPEC_MATCH" : "NAME_MATCH";
        } else if (categoryMatch) {
            rank = 1;
            matchType = "CATEGORY_MATCH";
            reason = "CATEGORY_MATCH";
        }
        return new SupplierScoredCandidate(
            candidate.withMatch(matchType, reason),
            rank,
            price(candidate.unitPrice()),
            stockSatisfied(candidate.stockQty(), requestedQty)
        );
    }

    private Comparator<SupplierScoredCandidate> supplierCandidateComparator() {
        return Comparator
            .comparingInt(SupplierScoredCandidate::rank).reversed()
            .thenComparing(SupplierScoredCandidate::stockSatisfied, Comparator.reverseOrder())
            .thenComparing(SupplierScoredCandidate::price)
            .thenComparing(candidate -> candidate.candidate().skuId(), Comparator.nullsLast(Long::compareTo));
    }

    private MatchDecision matchDemandInquiry(MaterialQuoteRow row, NormalizedMaterialName normalized, SearchCache searchCache) {
        String normalizedCode = normalizeCode(row.impaCode());
        List<String> nameTokens = materialTokens(normalized.nameTokens());
        List<String> specTokens = mergeTokens(materialTokens(normalized.specTokens()), tokenize(row.sizeModel()));
        List<String> searchTokens = mergeTokens(nameTokens, specTokens);
        List<String> riskFlags = normalized.riskFlags();

        if (normalizedCode.isBlank()) {
            List<MaterialMatchCandidate> candidates = nameSpecCandidates(searchTokens, nameTokens, specTokens, riskFlags, searchCache);
            return similarOrUnmatched(candidates, riskFlags);
        }
        if (!STANDARD_IMPA_CODE.matcher(normalizedCode).matches()) {
            List<MaterialMatchCandidate> candidates = nameSpecCandidates(searchTokens, nameTokens, specTokens, riskFlags, searchCache);
            return similarOrUnmatched(candidates, riskFlags);
        }

        List<ImpaItemResponse> codeHits = findExactCodeHits(normalizedCode, searchCache);
        if (codeHits.isEmpty()) {
            List<MaterialMatchCandidate> candidates = nameSpecCandidates(
                searchTokens,
                nameTokens,
                specTokens,
                riskFlags,
                searchCache
            );
            return similarOrUnmatched(candidates, riskFlags);
        }

        List<String> effectiveRiskFlags = riskFlags;
        if (isSupplierCodeCollision(row, normalizedCode)) {
            effectiveRiskFlags = withRiskFlag(riskFlags, "SUPPLIER_CODE_COLLISION");
        }
        ScoredCandidate codeScore = scoreCandidate(
            codeHits.get(0),
            nameTokens,
            specTokens,
            normalizeCode(codeHits.get(0).impaCode()),
            codeHits.get(0).categoryCode(),
            true,
            effectiveRiskFlags
        );
        if (effectiveRiskFlags.contains("SUPPLIER_CODE_COLLISION")
            && !nameTokens.isEmpty()
            && codeScore.nameScore() < 0.4) {
            List<MaterialMatchCandidate> candidates = nameSpecCandidates(
                searchTokens,
                nameTokens,
                specTokens,
                effectiveRiskFlags,
                searchCache
            );
            return similarOrUnmatched(candidates, effectiveRiskFlags);
        }

        List<MaterialMatchCandidate> candidates = candidatesFor(
            searchTokens,
            nameTokens,
            specTokens,
            codeHits,
            searchCache,
            normalizeCode(codeHits.get(0).impaCode()),
            codeHits.get(0).categoryCode(),
            true,
            effectiveRiskFlags
        );
        return new MatchDecision("EXACT", "Exact Match", REASON_CODE_MATCH, candidates, effectiveRiskFlags);
    }

    private MatchDecision matchSupplierQuotation(MaterialQuoteRow row, SearchCache searchCache) {
        List<String> nameTokens = tokenize(row.rawNameSpec());
        List<String> searchTokens = nameTokens;
        Map<String, ImpaItemResponse> seedItems = new LinkedHashMap<>();
        String supplierItemNo = normalizeCode(row.supplierItemNo());
        if (!supplierItemNo.isBlank()) {
            searchCache.findItems(supplierItemNo).forEach(item -> seedItems.putIfAbsent(item.impaCode(), item));
        }
        List<MaterialMatchCandidate> candidates = candidatesFor(
            searchTokens,
            nameTokens,
            List.of(),
            seedItems.values().stream().toList(),
            searchCache,
            "",
            null,
            false,
            List.of()
        );
        return similarOrUnmatched(candidates, List.of());
    }

    private MatchDecision matchUnknown(MaterialQuoteRow row, SearchCache searchCache) {
        List<String> searchTokens = tokenize(String.join(" ", row.rawColumns().values()));
        List<MaterialMatchCandidate> candidates = nameSpecCandidates(searchTokens, searchTokens, List.of(), List.of(), searchCache);
        return similarOrUnmatched(candidates, List.of());
    }

    private MatchDecision similarOrUnmatched(List<MaterialMatchCandidate> candidates, List<String> riskFlags) {
        if (candidates.isEmpty()) {
            return new MatchDecision("UNMATCHED", "Unmatched", REASON_NAME_SPEC_MISMATCH, List.of(), riskFlags);
        }
        String reason = candidates.get(0).reason();
        if (REASON_NAME_SPEC_MISMATCH.equals(reason)) {
            return new MatchDecision("UNMATCHED", "Unmatched", reason, candidates, riskFlags);
        }
        return new MatchDecision("SIMILAR", "Similar Match", reason, candidates, riskFlags);
    }

    private List<MaterialMatchCandidate> nameSpecCandidates(
        List<String> searchTokens,
        List<String> nameTokens,
        List<String> specTokens,
        List<String> riskFlags,
        SearchCache searchCache
    ) {
        return candidatesFor(searchTokens, nameTokens, specTokens, List.of(), searchCache, "", null, false, riskFlags);
    }

    private List<MaterialMatchCandidate> candidatesFor(
        List<String> searchTokens,
        List<String> nameTokens,
        List<String> specTokens,
        List<ImpaItemResponse> seedItems,
        SearchCache searchCache,
        String exactCode,
        String preferredCategoryCode,
        boolean officialCodeMode,
        List<String> riskFlags
    ) {
        Map<String, ImpaItemResponse> unique = new LinkedHashMap<>();
        seedItems.forEach(item -> unique.putIfAbsent(item.impaCode(), item));
        addKeywordCandidates(unique, searchTokens, searchCache);
        if (unique.isEmpty()) {
            return List.of();
        }

        String categoryCode = preferredCategoryCode == null
            ? inferPreferredCategory(unique.values().stream().toList(), nameTokens, specTokens)
            : preferredCategoryCode;
        List<ScoredCandidate> scored = unique.values().stream()
            .map(item -> scoreCandidate(item, nameTokens, specTokens, exactCode, categoryCode, officialCodeMode, riskFlags))
            .toList();

        List<ScoredCandidate> sameCategory = scored.stream()
            .filter(ScoredCandidate::sameCategory)
            .sorted(candidateComparator())
            .toList();
        List<ScoredCandidate> ordered = new ArrayList<>(sameCategory);
        if (ordered.size() < MAX_CANDIDATES) {
            scored.stream()
                .filter(candidate -> !candidate.sameCategory())
                .sorted(candidateComparator())
                .forEach(ordered::add);
        }

        return ordered.stream()
            .limit(MAX_CANDIDATES)
            .map(ScoredCandidate::candidate)
            .toList();
    }

    private String inferPreferredCategory(
        List<ImpaItemResponse> items,
        List<String> nameTokens,
        List<String> specTokens
    ) {
        return items.stream()
            .map(item -> scoreCandidate(item, nameTokens, specTokens, "", null, false, List.of()))
            .filter(candidate -> candidate.nameScore() > 0 || candidate.specScore() > 0)
            .max(Comparator
                .comparingDouble(ScoredCandidate::nameScore)
                .thenComparingDouble(ScoredCandidate::specScore))
            .map(candidate -> candidate.candidate().categoryCode())
            .orElse(null);
    }

    private ScoredCandidate scoreCandidate(
        ImpaItemResponse item,
        List<String> nameTokens,
        List<String> specTokens,
        String exactCode,
        String preferredCategoryCode,
        boolean officialCodeMode,
        List<String> riskFlags
    ) {
        double nameScore = coverage(nameTokens, mergeTokens(tokenize(item.nameEn()), tokenize(item.nameCn())));
        double specScore = specTokens.isEmpty() ? 1.0 : specCoverage(specTokens, tokenize(item.specification()));
        boolean codeMatch = !exactCode.isBlank() && exactCode.equals(normalizeCode(item.impaCode()));
        boolean sameCategory = preferredCategoryCode != null && preferredCategoryCode.equals(item.categoryCode());
        boolean nameMatch = !nameTokens.isEmpty() && nameScore >= FULL_MATCH_NAME_THRESHOLD;
        boolean specMatch = specTokens.isEmpty() || specScore >= FULL_MATCH_SPEC_THRESHOLD;
        String candidateMatchType = codeMatch && officialCodeMode ? "CODE_MATCH" : "SPEC_MATCH";
        String reason = reasonFor(codeMatch && officialCodeMode, nameMatch, nameScore, specMatch, riskFlags);
        MaterialMatchCandidate candidate = new MaterialMatchCandidate(
            item.impaCode(),
            item.categoryCode(),
            item.categoryName(),
            item.nameCn(),
            item.nameEn(),
            item.specification(),
            item.unit(),
            candidateMatchType,
            reason
        );
        return new ScoredCandidate(candidate, codeMatch, sameCategory, nameScore, specScore);
    }

    private String reasonFor(
        boolean officialCodeMatch,
        boolean nameMatch,
        double nameScore,
        boolean specMatch,
        List<String> riskFlags
    ) {
        if (officialCodeMatch) {
            return REASON_CODE_MATCH;
        }
        boolean manualReviewRisk = riskFlags.stream().anyMatch(flag ->
            "MULTI_PRODUCT_FAMILY".equals(flag)
                || "KIT_OR_SET".equals(flag)
                || "MULTIPLE_SIZE_GROUPS".equals(flag)
                || "SUPPLIER_CODE_COLLISION".equals(flag)
        );
        if (nameMatch && specMatch && !manualReviewRisk) {
            return REASON_NAME_SPEC_MATCH;
        }
        if (nameMatch) {
            return REASON_NAME_MATCH_SPEC_CHECK;
        }
        if (nameScore > 0) {
            return REASON_NAME_MATCH_LOW_CONFIDENCE;
        }
        return REASON_NAME_SPEC_MISMATCH;
    }

    private Comparator<ScoredCandidate> candidateComparator() {
        return Comparator
            .comparingInt((ScoredCandidate candidate) -> matchTypeRank(candidate.candidate().candidateMatchType())).reversed()
            .thenComparing(ScoredCandidate::codeMatch, Comparator.reverseOrder())
            .thenComparing(ScoredCandidate::sameCategory, Comparator.reverseOrder())
            .thenComparingDouble(ScoredCandidate::nameScore).reversed()
            .thenComparingDouble(ScoredCandidate::specScore).reversed()
            .thenComparing(candidate -> candidate.candidate().categoryCode(), Comparator.nullsLast(String::compareTo))
            .thenComparing(candidate -> candidate.candidate().impaCode());
    }

    private int matchTypeRank(String matchType) {
        return switch (matchType) {
            case "FULL_MATCH" -> 3;
            case "CODE_MATCH" -> 2;
            case "SPEC_MATCH" -> 1;
            default -> 0;
        };
    }

    private List<ImpaItemResponse> findExactCodeHits(String code, SearchCache searchCache) {
        return searchCache.findItems(code).stream()
            .filter(item -> normalizeCode(item.impaCode()).equals(code))
            .toList();
    }

    private void addKeywordCandidates(
        Map<String, ImpaItemResponse> unique,
        List<String> searchTokens,
        SearchCache searchCache
    ) {
        for (String token : searchTokens.stream().limit(8).toList()) {
            for (ImpaItemResponse item : searchCache.findItems(token)) {
                unique.putIfAbsent(item.impaCode(), item);
                if (unique.size() >= 40) {
                    break;
                }
            }
            if (unique.size() >= 40) {
                break;
            }
        }
    }

    private double coverage(List<String> sourceTokens, List<String> candidateTokens) {
        if (sourceTokens.isEmpty() || candidateTokens.isEmpty()) {
            return 0;
        }
        Set<String> candidateSet = new LinkedHashSet<>(candidateTokens);
        long overlap = sourceTokens.stream().filter(candidateSet::contains).count();
        return (double) overlap / sourceTokens.size();
    }

    private double specCoverage(List<String> sourceTokens, List<String> candidateTokens) {
        if (sourceTokens.isEmpty() || candidateTokens.isEmpty()) {
            return 0;
        }
        Set<String> candidateSet = new LinkedHashSet<>(candidateTokens);
        return sourceTokens.stream().anyMatch(candidateSet::contains) ? 1.0 : 0;
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : TOKEN_SPLITTER.split(value.toUpperCase(Locale.ROOT))) {
            String normalized = normalizeMaterialToken(token);
            if (normalized.length() < 3 || STOP_WORDS.contains(normalized) || normalized.chars().allMatch(Character::isDigit)) {
                continue;
            }
            tokens.add(normalized);
        }
        return tokens.stream().distinct().toList();
    }

    private List<String> mergeTokens(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged.stream().distinct().toList();
    }

    private List<String> materialTokens(List<String> tokens) {
        return tokens.stream()
            .map(token -> normalizeMaterialToken(token.toUpperCase(Locale.ROOT)))
            .filter(token -> token.length() >= 3)
            .filter(token -> !STOP_WORDS.contains(token))
            .distinct()
            .toList();
    }

    private String normalizeMaterialToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String upper = token.toUpperCase(Locale.ROOT);
        if (upper.endsWith("IES") && upper.length() > 4) {
            return upper.substring(0, upper.length() - 3) + "Y";
        }
        if (upper.endsWith("ES") && upper.length() > 4 && !upper.endsWith("SS")) {
            return upper.substring(0, upper.length() - 2);
        }
        if (upper.endsWith("S") && upper.length() > 4 && !upper.endsWith("SS") && !upper.endsWith("US")) {
            return upper.substring(0, upper.length() - 1);
        }
        return upper;
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private void addCode(Set<String> codes, String value) {
        String normalized = normalizeCode(value);
        if (!normalized.isBlank()) {
            codes.add(normalized);
        }
    }

    private boolean isSupplierCodeCollision(MaterialQuoteRow row, String normalizedCode) {
        return !normalizedCode.isBlank()
            && !normalizeCode(row.supplierItemNo()).isBlank()
            && normalizedCode.equals(normalizeCode(row.supplierItemNo()));
    }

    private List<String> withRiskFlag(List<String> riskFlags, String flag) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(riskFlags);
        merged.add(flag);
        return List.copyOf(merged);
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal price(BigDecimal value) {
        return value == null ? new BigDecimal("999999999") : value;
    }

    private boolean stockSatisfied(BigDecimal stockQty, BigDecimal requestedQty) {
        return stockQty != null && requestedQty != null && stockQty.compareTo(requestedQty) >= 0;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private class SearchCache {

        private final Map<String, List<ImpaItemResponse>> itemsByKeyword = new LinkedHashMap<>();
        private List<SearchableImpaItem> searchableItems;
        private Map<String, ImpaItemResponse> itemsByCode;

        private List<ImpaItemResponse> findItems(String keyword) {
            return itemsByKeyword.computeIfAbsent(keyword, this::findItemsInMemory);
        }

        private List<ImpaItemResponse> findItemsInMemory(String keyword) {
            String normalizedKeyword = keyword.toUpperCase(Locale.ROOT);
            String normalizedCode = normalizeCode(keyword);
            List<ImpaItemResponse> matches = new ArrayList<>();
            ImpaItemResponse exactCodeItem = itemsByCode().get(normalizedCode);
            if (exactCodeItem != null) {
                matches.add(exactCodeItem);
            }
            for (SearchableImpaItem searchable : searchableItems()) {
                if (matches.size() >= MAX_CANDIDATES) {
                    break;
                }
                ImpaItemResponse item = searchable.item();
                if (matches.stream().anyMatch(match -> match.impaCode().equals(item.impaCode()))) {
                    continue;
                }
                if (searchable.haystack().contains(normalizedKeyword)) {
                    matches.add(item);
                }
            }
            return matches;
        }

        private List<SearchableImpaItem> searchableItems() {
            if (searchableItems == null) {
                searchableItems = impaItemRepository.findItems(null, null, null, 60000).stream()
                    .map(item -> new SearchableImpaItem(item, haystack(item)))
                    .toList();
                itemsByCode = searchableItems.stream()
                    .map(SearchableImpaItem::item)
                    .collect(java.util.stream.Collectors.toMap(
                        item -> normalizeCode(item.impaCode()),
                        item -> item,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                    ));
            }
            return searchableItems;
        }

        private Map<String, ImpaItemResponse> itemsByCode() {
            searchableItems();
            return itemsByCode;
        }

        private String haystack(ImpaItemResponse item) {
            return String.join(
                " ",
                nullToEmpty(item.impaCode()),
                nullToEmpty(item.nameCn()),
                nullToEmpty(item.nameEn()),
                nullToEmpty(item.specification())
            ).toUpperCase(Locale.ROOT);
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }

    private record SearchableImpaItem(ImpaItemResponse item, String haystack) {
    }

    private record MatchDecision(
        String matchResult,
        String matchResultName,
        String reason,
        List<MaterialMatchCandidate> candidates,
        List<String> riskFlags
    ) {
    }

    private record ValidationDecision(
        String status,
        String reason
    ) {
    }

    private record ScoredCandidate(
        MaterialMatchCandidate candidate,
        boolean codeMatch,
        boolean sameCategory,
        double nameScore,
        double specScore
    ) {
    }

    private record SupplierScoredCandidate(
        MaterialSupplierCandidate candidate,
        int rank,
        BigDecimal price,
        boolean stockSatisfied
    ) {
    }
}
