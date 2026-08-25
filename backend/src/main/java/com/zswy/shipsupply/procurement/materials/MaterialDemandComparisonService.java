package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@Service
public class MaterialDemandComparisonService {

    private static final int MAX_CANDIDATES_PER_ITEM = 10;
    private static final String CNY = "CNY";
    private static final BigDecimal USD_RATE = new BigDecimal("7");
    private static final BigDecimal MISSING_PRICE = new BigDecimal("999999999");
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
        "AND", "THE", "FOR", "WITH", "WITHOUT", "OF", "IN", "ON", "A", "AN",
        "NO", "TYPE", "PCS", "PC", "SET", "BOX", "CTN", "INNER", "OUTER",
        "CM", "MM", "KG", "KGS", "RMB", "FOB", "WAREHOUSE", "MODEL", "SIZE"
    );

    private final CurrentUserService currentUserService;
    private final MaterialDemandRepository materialDemandRepository;
    private final MaterialSupplierCandidateProvider supplierCandidateProvider;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MaterialComparisonModelReranker modelReranker;
    private final MaterialComparisonStrategyRepository strategyRepository;

    MaterialDemandComparisonService(
        CurrentUserService currentUserService,
        MaterialDemandRepository materialDemandRepository,
        MaterialSupplierCandidateProvider supplierCandidateProvider,
        PurchaseOrderRepository purchaseOrderRepository
    ) {
        this(
            currentUserService,
            materialDemandRepository,
            supplierCandidateProvider,
            purchaseOrderRepository,
            new MaterialComparisonModelReranker(
                new com.fasterxml.jackson.databind.ObjectMapper(), false, "https://api.openai.com/v1", "", "gpt-5-mini"
            ),
            null
        );
    }

    @Autowired
    public MaterialDemandComparisonService(
        CurrentUserService currentUserService,
        MaterialDemandRepository materialDemandRepository,
        MaterialSupplierCandidateProvider supplierCandidateProvider,
        PurchaseOrderRepository purchaseOrderRepository,
        MaterialComparisonModelReranker modelReranker,
        MaterialComparisonStrategyRepository strategyRepository
    ) {
        this.currentUserService = currentUserService;
        this.materialDemandRepository = materialDemandRepository;
        this.supplierCandidateProvider = supplierCandidateProvider;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.modelReranker = modelReranker;
        this.strategyRepository = strategyRepository;
    }

    public MaterialDemandComparisonResponse comparison(String authorizationHeader, Long demandId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSummaryResponse demand = requireDemand(currentUser.companyId(), demandId);
        if ("DISCARDED".equalsIgnoreCase(demand.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MATERIAL_DEMAND_DISCARDED");
        }
        materialDemandRepository.markComparingIfSaved(currentUser.companyId(), demandId);
        List<MaterialDemandItemResponse> demandItems = materialDemandRepository.items(currentUser.companyId(), demandId);
        MaterialComparisonStrategySettings settings = loadSettings(currentUser.companyId(), demandId);
        SupplierCandidateIndex supplierPool = SupplierCandidateIndex.from(onShelfSupplierPool(demandItems));
        Long existingPurchaseOrderId = purchaseOrderRepository.findFirstActiveOrderIdByDemand(currentUser.companyId(), demandId);
        if (existingPurchaseOrderId != null && existingPurchaseOrderId <= 0) {
            existingPurchaseOrderId = null;
        }

        List<ComparisonItemDraft> drafts = demandItems.stream()
            .map(item -> draftItem(item, supplierPool, settings))
            .toList();
        List<MaterialSupplierCandidate> enrichedPool = enrichShortlistedCandidates(drafts);
        if (!enrichedPool.isEmpty()) drafts = enrichDrafts(drafts, enrichedPool, settings);
        MixedSupplierSelection mixedSupplier = chooseMixedSuppliers(drafts, settings);
        SingleSupplierSelection singleSupplier = chooseSingleSupplier(drafts, settings);
        List<MaterialDemandComparisonItem> items = drafts.stream()
            .map(draft -> draft.toResponse(
                mixedSupplier.candidateByItemId().get(draft.item().itemId()),
                singleSupplier.candidateByItemId().get(draft.item().itemId())
            ))
            .toList();

        return new MaterialDemandComparisonResponse(
            demand,
            new MaterialDemandSupplyInfo(
                demand.vesselName(),
                firstNonBlank(demand.supplyPortName(), demand.supplyPortCode(), "待补充"),
                demand.supplyPortCode(),
                demand.supplyPortName(),
                demand.vesselEta(),
                firstNonBlank(datePart(demand.vesselEta()), demand.inquiryDate()),
                "天气待接入",
                "STATIC_PLACEHOLDER"
            ),
            List.of(mixedSupplier.strategy(), singleSupplier.strategy()),
            items,
            existingPurchaseOrderId != null || "ORDERED".equalsIgnoreCase(demand.status()),
            "DISCARDED".equalsIgnoreCase(demand.status()),
            existingPurchaseOrderId,
            aiSummary(drafts),
            settings
        );
    }

    public MaterialComparisonStrategySettings strategySettings(String authorizationHeader, Long demandId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireDemand(currentUser.companyId(), demandId);
        return loadSettings(currentUser.companyId(), demandId);
    }

    public MaterialComparisonStrategySettings saveStrategySettings(
        String authorizationHeader,
        Long demandId,
        MaterialComparisonStrategySettingsRequest request
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSummaryResponse demand = requireDemand(currentUser.companyId(), demandId);
        Long existingPurchaseOrderId = purchaseOrderRepository.findFirstActiveOrderIdByDemand(currentUser.companyId(), demandId);
        if ("ORDERED".equalsIgnoreCase(demand.status()) || "DISCARDED".equalsIgnoreCase(demand.status())
            || existingPurchaseOrderId != null && existingPurchaseOrderId > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MATERIAL_COMPARISON_STRATEGY_READONLY");
        }
        MaterialComparisonStrategySettings normalized = normalizeSettings(request);
        Set<Long> itemIds = materialDemandRepository.items(currentUser.companyId(), demandId).stream()
            .map(MaterialDemandItemResponse::itemId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        if (!itemIds.containsAll(normalized.coreDemandItemIds())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATERIAL_COMPARISON_CORE_ITEM_INVALID");
        }
        return strategyRepository == null
            ? normalized
            : strategyRepository.save(currentUser.companyId(), demandId, currentUser.userId(), normalized);
    }

    private MaterialComparisonStrategySettings loadSettings(Long companyId, Long demandId) {
        return strategyRepository == null
            ? MaterialComparisonStrategySettings.defaults()
            : strategyRepository.find(companyId, demandId).orElse(MaterialComparisonStrategySettings.defaults());
    }

    private MaterialComparisonStrategySettings normalizeSettings(MaterialComparisonStrategySettingsRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATERIAL_COMPARISON_STRATEGY_REQUIRED");
        }
        int supplierCount = request.mixedSupplierCount() == null ? 3 : request.mixedSupplierCount();
        boolean priceEnabled = request.priceEnabled() == null || request.priceEnabled();
        boolean qualityEnabled = request.qualityEnabled() == null || request.qualityEnabled();
        int priceLevel = request.priceLevel() == null ? 5 : request.priceLevel();
        int qualityLevel = request.qualityLevel() == null ? 3 : request.qualityLevel();
        if (supplierCount < 2 || supplierCount > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATERIAL_COMPARISON_SUPPLIER_COUNT_INVALID");
        }
        if (!priceEnabled && !qualityEnabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATERIAL_COMPARISON_OBJECTIVE_REQUIRED");
        }
        if (priceLevel < 1 || priceLevel > 5 || qualityLevel < 1 || qualityLevel > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATERIAL_COMPARISON_LEVEL_INVALID");
        }
        List<Long> coreItemIds = request.coreDemandItemIds() == null ? List.of() : request.coreDemandItemIds().stream()
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        return new MaterialComparisonStrategySettings(
            supplierCount, priceEnabled, priceLevel, qualityEnabled, qualityLevel, coreItemIds, 1
        );
    }

    public List<MaterialSupplierCandidate> itemSupplierCandidates(String authorizationHeader, Long demandId, Long itemId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSummaryResponse demand = requireDemand(currentUser.companyId(), demandId);
        if ("DISCARDED".equalsIgnoreCase(demand.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MATERIAL_DEMAND_DISCARDED");
        }
        MaterialDemandItemResponse item = materialDemandRepository.items(currentUser.companyId(), demandId).stream()
            .filter(candidate -> itemId != null && itemId.equals(candidate.itemId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MATERIAL_DEMAND_ITEM_NOT_FOUND"));
        ComparisonItemDraft draft = draftItem(
            item,
            SupplierCandidateIndex.from(onShelfSupplierPool(List.of(item))),
            MaterialComparisonStrategySettings.defaults()
        );
        List<MaterialSupplierCandidate> enriched = enrichShortlistedCandidates(List.of(draft));
        return enriched.isEmpty()
            ? draft.candidates()
            : enrichDrafts(List.of(draft), enriched, MaterialComparisonStrategySettings.defaults()).get(0).candidates();
    }

    private List<MaterialSupplierCandidate> onShelfSupplierPool(List<MaterialDemandItemResponse> items) {
        List<MaterialSupplierCandidate> candidates = supplierCandidateProvider.supportsTargetedSearch()
            ? supplierCandidateProvider.findCandidates(candidateQuery(items))
            : supplierCandidateProvider.findOnShelfCandidates();
        return (candidates == null ? List.<MaterialSupplierCandidate>of() : candidates).stream()
            .filter(candidate -> "ON_SHELF".equalsIgnoreCase(nullToEmpty(candidate.shelfStatus())))
            .toList();
    }

    private List<MaterialSupplierCandidate> enrichShortlistedCandidates(List<ComparisonItemDraft> drafts) {
        if (!supplierCandidateProvider.supportsTargetedSearch()) {
            return List.of();
        }
        Set<Long> skuIds = drafts.stream()
            .flatMap(draft -> draft.candidates().stream())
            .map(MaterialSupplierCandidate::skuId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (skuIds.isEmpty()) {
            return List.of();
        }
        List<MaterialSupplierCandidate> enriched = supplierCandidateProvider.enrichCandidates(skuIds);
        return enriched == null ? List.of() : enriched;
    }

    private MaterialSupplierCandidateQuery candidateQuery(List<MaterialDemandItemResponse> items) {
        Set<String> codes = new LinkedHashSet<>();
        Set<String> supplierSkuCodes = new LinkedHashSet<>();
        Set<String> categoryCodes = new LinkedHashSet<>();
        List<String> keywords = new ArrayList<>();
        for (MaterialDemandItemResponse item : items == null ? List.<MaterialDemandItemResponse>of() : items) {
            addCode(codes, item.impaCode());
            addCode(codes, item.selectedImpaCode());
            addCode(codes, item.candidateImpaCode());
            addCode(supplierSkuCodes, item.supplierItemNo());
            String category = preferredCategory(item);
            if (category != null && !category.isBlank()) {
                categoryCodes.add(category);
            }
            keywords.addAll(searchTerms(item.description()));
            keywords.addAll(searchTerms(item.rawNameSpec()));
            keywords.addAll(searchTerms(item.candidateNameCn()));
            keywords.addAll(searchTerms(item.candidateNameEn()));
            keywords.addAll(searchTerms(item.sizeModel()));
            keywords.addAll(searchTerms(item.candidateSpec()));
            keywords.addAll(searchTerms(item.packing()));
            keywords.addAll(searchTerms(item.unit()));
        }
        return new MaterialSupplierCandidateQuery(codes, supplierSkuCodes, categoryCodes, keywords);
    }

    private MaterialDemandSummaryResponse requireDemand(Long companyId, Long demandId) {
        if (demandId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "demandId is required");
        }
        return materialDemandRepository.findSummaryById(companyId, demandId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MATERIAL_DEMAND_NOT_FOUND"));
    }

    private ComparisonItemDraft draftItem(
        MaterialDemandItemResponse item,
        SupplierCandidateIndex supplierPool,
        MaterialComparisonStrategySettings settings
    ) {
        BigDecimal pricingQuantity = parsePositiveQuantity(item.quantity());
        String pricingQuantityNote = pricingQuantity == null ? "\u8ba1\u4ef7\u6570\u91cf\u6309 1" : null;
        BigDecimal safePricingQuantity = pricingQuantity == null ? BigDecimal.ONE : pricingQuantity;
        MaterialComparisonModelReranker.RerankResult rerank = supplierCandidates(item, supplierPool, safePricingQuantity);
        List<MaterialSupplierCandidate> candidates = rerank.candidates();
        MaterialSupplierCandidate lowest = candidates.stream()
            .filter(candidate -> candidate.unitPrice() != null)
            .min(strategyCandidateComparator(settings))
            .orElse(null);
        String emptyReason = null;
        if (candidates.isEmpty()) {
            emptyReason = "NO_SUPPLIER_CANDIDATE";
        } else if (lowest == null) {
            emptyReason = "NO_PRICED_CANDIDATE";
        }
        return new ComparisonItemDraft(item, safePricingQuantity, pricingQuantityNote, candidates, lowest, emptyReason, rerank.status());
    }

    private MaterialComparisonModelReranker.RerankResult supplierCandidates(
        MaterialDemandItemResponse item,
        SupplierCandidateIndex supplierPool,
        BigDecimal requestedQty
    ) {
        List<String> nameTokens = mergeTokens(
            tokenize(item.description()),
            mergeTokens(tokenize(item.rawNameSpec()), mergeTokens(tokenize(item.candidateNameEn()), tokenize(item.candidateNameCn())))
        );
        List<String> specTokens = mergeTokens(tokenize(item.sizeModel()), mergeTokens(tokenize(item.candidateSpec()), tokenize(item.packing())));
        String supplierSku = normalizeCode(item.supplierItemNo());
        Set<String> platformCodes = new LinkedHashSet<>();
        addCode(platformCodes, item.impaCode());
        addCode(platformCodes, item.selectedImpaCode());
        addCode(platformCodes, item.candidateImpaCode());
        String preferredCategory = preferredCategory(item);

        List<IndexedSupplierCandidate> candidatesToScore = supplierPool.candidatesForEvidence(
            platformCodes,
            supplierSku,
            nameTokens,
            preferredCategory
        );

        List<MaterialSupplierCandidate> deterministicCandidates = candidatesToScore.stream()
            .map(candidate -> scoreCandidate(candidate, supplierSku, platformCodes, nameTokens, specTokens, preferredCategory, requestedQty))
            .filter(candidate -> candidate.rank() > 0)
            .sorted(candidateComparator())
            .limit(MAX_CANDIDATES_PER_ITEM)
            .map(candidate -> pricedCandidate(candidate.candidate(), requestedQty))
            .toList();
        return modelReranker.rerankWithStatus(item, deterministicCandidates);
    }

    private List<ComparisonItemDraft> enrichDrafts(
        List<ComparisonItemDraft> drafts,
        List<MaterialSupplierCandidate> enrichedCandidates,
        MaterialComparisonStrategySettings settings
    ) {
        Map<Long, MaterialSupplierCandidate> enrichedById = enrichedCandidates.stream()
            .filter(candidate -> candidate.skuId() != null)
            .collect(Collectors.toMap(MaterialSupplierCandidate::skuId, Function.identity(), (first, ignored) -> first));
        return drafts.stream().map(draft -> {
            List<MaterialSupplierCandidate> candidates = draft.candidates().stream().map(candidate -> {
                MaterialSupplierCandidate enriched = enrichedById.get(candidate.skuId());
                return pricedCandidate(enriched == null ? candidate : enriched.withMatch(candidate.matchType(), candidate.reason()), draft.pricingQuantity());
            }).toList();
            MaterialSupplierCandidate lowest = candidates.stream()
                .filter(candidate -> candidate.unitPrice() != null)
                .min(strategyCandidateComparator(settings))
                .orElse(null);
            String emptyReason = candidates.isEmpty() ? "NO_SUPPLIER_CANDIDATE" : lowest == null ? "NO_PRICED_CANDIDATE" : null;
            return new ComparisonItemDraft(
                draft.item(), draft.pricingQuantity(), draft.pricingQuantityNote(), candidates, lowest, emptyReason, draft.aiStatus()
            );
        }).toList();
    }

    private MaterialComparisonAiSummary aiSummary(List<ComparisonItemDraft> drafts) {
        int applied = (int) drafts.stream().filter(draft -> "MODEL_APPLIED".equals(draft.aiStatus())).count();
        int fallback = (int) drafts.stream().filter(draft -> "MODEL_FALLBACK".equals(draft.aiStatus())).count();
        boolean configurationRequired = drafts.stream().anyMatch(draft -> "MODEL_CONFIGURATION_REQUIRED".equals(draft.aiStatus()));
        String status = fallback > 0 ? "MODEL_FALLBACK"
            : applied > 0 ? "MODEL_APPLIED"
            : configurationRequired ? "MODEL_CONFIGURATION_REQUIRED"
            : "DETERMINISTIC";
        return new MaterialComparisonAiSummary(status, applied, fallback);
    }

    private ScoredSupplierCandidate scoreCandidate(
        IndexedSupplierCandidate indexedCandidate,
        String supplierSku,
        Set<String> platformCodes,
        List<String> nameTokens,
        List<String> specTokens,
        String preferredCategory,
        BigDecimal requestedQty
    ) {
        MaterialSupplierCandidate candidate = indexedCandidate.candidate();
        boolean rawCodeMatch = platformCodes.stream()
            .anyMatch(code -> code.equals(normalizeCode(candidate.impaCode())) || code.equals(normalizeCode(candidate.platformCode())));
        boolean supplierSkuMatch = !supplierSku.isBlank() && supplierSku.equals(normalizeCode(candidate.supplierSkuCode()));
        double nameScore = coverage(nameTokens, indexedCandidate.nameTokens());
        double specScore = coverage(specTokens, indexedCandidate.specTokens());
        boolean nameSpecMatch = nameScore >= 0.6 || (!nameTokens.isEmpty() && nameScore > 0 && (specTokens.isEmpty() || specScore > 0));
        boolean categoryMatch = preferredCategory != null && preferredCategory.equals(candidate.categoryCode());
        boolean trustedCode = trustedCodeStatus(candidate.codeStatus());
        boolean specificationConflict = rawCodeMatch
            && trustedCode
            && !specTokens.isEmpty()
            && !indexedCandidate.specTokens().isEmpty()
            && hasComparableSpecification(specTokens)
            && hasComparableSpecification(indexedCandidate.specTokens())
            && specScore == 0;

        int rank = 0;
        String matchType = null;
        String reason = null;
        if (rawCodeMatch && trustedCode && !specificationConflict) {
            rank = 4;
            matchType = "CODE_EXACT";
            reason = "IMPA_OR_PLATFORM_CODE_MATCH";
        } else if (supplierSkuMatch) {
            rank = 3;
            matchType = "SUPPLIER_SKU_MATCH";
            reason = "SUPPLIER_SKU_MATCH";
        } else if (specificationConflict) {
            rank = 1;
            matchType = "CODE_SPEC_CONFLICT";
            reason = "CODE_MATCH_SPECIFICATION_CONFLICT";
        } else if (nameSpecMatch) {
            rank = 2;
            matchType = "NAME_SPEC_MATCH";
            reason = specTokens.isEmpty() || specScore > 0 ? "NAME_SPEC_MATCH" : "NAME_MATCH";
        } else if (categoryMatch) {
            rank = 1;
            matchType = "CATEGORY_MATCH";
            reason = "CATEGORY_MATCH";
        }
        return new ScoredSupplierCandidate(
            candidate.withMatch(matchType, reason),
            rank,
            price(candidate.unitPrice()),
            stockSatisfied(candidate.stockQty(), requestedQty)
        );
    }

    private Comparator<ScoredSupplierCandidate> candidateComparator() {
        return Comparator
            .comparingInt(ScoredSupplierCandidate::rank).reversed()
            .thenComparing(ScoredSupplierCandidate::stockSatisfied, Comparator.reverseOrder())
            .thenComparing(ScoredSupplierCandidate::price)
            .thenComparing(candidate -> candidate.candidate().skuId(), Comparator.nullsLast(Long::compareTo));
    }

    private Comparator<MaterialSupplierCandidate> strategyCandidateComparator(MaterialComparisonStrategySettings settings) {
        return Comparator
            .comparingInt((MaterialSupplierCandidate candidate) -> matchPriority(candidate.matchType())).reversed()
            .thenComparing(candidate -> stockSatisfied(candidate.stockQty(), BigDecimal.ONE), Comparator.reverseOrder())
            .thenComparing(Comparator.comparingInt(
                (MaterialSupplierCandidate candidate) -> weightedProductScore(candidate, settings)
            ).reversed())
            .thenComparing(candidate -> price(candidate.unitPrice()))
            .thenComparing(MaterialSupplierCandidate::skuId, Comparator.nullsLast(Long::compareTo));
    }

    private int matchPriority(String matchType) {
        return switch (nullToEmpty(matchType).toUpperCase(Locale.ROOT)) {
            case "CODE_EXACT" -> 4;
            case "SUPPLIER_SKU_MATCH" -> 3;
            case "NAME_SPEC_MATCH" -> 2;
            case "CATEGORY_MATCH", "CODE_SPEC_CONFLICT" -> 1;
            default -> 0;
        };
    }

    private int weightedProductScore(MaterialSupplierCandidate candidate, MaterialComparisonStrategySettings settings) {
        if (candidate == null) {
            return 0;
        }
        int weight = 0;
        int score = 0;
        if (settings.priceEnabled()) {
            weight += settings.priceLevel();
            score += settings.priceLevel() * clampScore(candidate.priceScore());
        }
        if (settings.qualityEnabled()) {
            weight += settings.qualityLevel();
            score += settings.qualityLevel() * clampScore(candidate.qualityScore());
        }
        return weight == 0 ? 0 : Math.round((float) score / weight * 20);
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(5, value));
    }

    private MixedSupplierSelection chooseMixedSuppliers(
        List<ComparisonItemDraft> drafts,
        MaterialComparisonStrategySettings settings
    ) {
        List<SupplierKey> available = drafts.stream()
            .flatMap(draft -> draft.candidates().stream())
            .filter(candidate -> strategyCandidateEligible(candidate, settings))
            .map(SupplierKey::from)
            .distinct()
            .toList();
        List<SupplierKey> selected = new ArrayList<>();
        while (selected.size() < settings.mixedSupplierCount() && selected.size() < available.size()) {
            SupplierKey next = available.stream()
                .filter(candidate -> !selected.contains(candidate))
                .max((left, right) -> comparePortfolio(
                    portfolioScore(drafts, append(selected, left), settings),
                    portfolioScore(drafts, append(selected, right), settings)
                ))
                .orElse(null);
            if (next == null) break;
            selected.add(next);
        }
        Map<Long, MaterialSupplierCandidate> assignments = portfolioAssignments(drafts, selected, settings);
        ensureEachSelectedSupplierContributes(drafts, selected, assignments, settings);
        Set<Long> coreIds = Set.copyOf(settings.coreDemandItemIds());
        Map<SupplierKey, List<Map.Entry<Long, MaterialSupplierCandidate>>> bySupplier = assignments.entrySet().stream()
            .collect(Collectors.groupingBy(
                entry -> SupplierKey.from(entry.getValue()),
                java.util.LinkedHashMap::new,
                Collectors.toList()
            ));
        List<MaterialDemandComparisonSupplier> suppliers = bySupplier.entrySet().stream()
            .map(entry -> supplierSummary(entry.getKey(), entry.getValue(), drafts, coreIds))
            .sorted(Comparator
                .comparing(MaterialDemandComparisonSupplier::totalAmount)
                .thenComparing(MaterialDemandComparisonSupplier::supplierName, Comparator.nullsLast(String::compareTo)))
            .toList();
        int coreCovered = (int) coreIds.stream().filter(assignments::containsKey).count();
        boolean coreComplete = coreCovered == coreIds.size();
        boolean enabled = suppliers.size() >= 2 && coreComplete;
        String disabledReason = !coreComplete ? "CORE_ITEM_UNCOVERED" : suppliers.size() < 2 ? "ONLY_ONE_SUPPLIER" : null;
        List<ComparisonItemDraft> priced = drafts.stream().filter(draft -> assignments.containsKey(draft.item().itemId())).toList();
        BigDecimal total = assignments.entrySet().stream()
            .map(entry -> lineAmount(entry.getValue(), draftByItemId(drafts, entry.getKey()).pricingQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUsd = assignments.entrySet().stream()
            .map(entry -> lineAmountUsd(entry.getValue(), draftByItemId(drafts, entry.getKey()).pricingQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MixedSupplierSelection(
            new MaterialDemandComparisonStrategy(
                "LOWEST_MIXED", "最低混供", priced.size(), drafts.size(), drafts.size() - priced.size(),
                0, total, totalUsd, CNY, suppliers, enabled, disabledReason, objectiveTags(settings)
            ),
            assignments
        );
    }

    private List<SupplierKey> append(List<SupplierKey> selected, SupplierKey candidate) {
        List<SupplierKey> result = new ArrayList<>(selected);
        result.add(candidate);
        return result;
    }

    private PortfolioScore portfolioScore(
        List<ComparisonItemDraft> drafts,
        List<SupplierKey> suppliers,
        MaterialComparisonStrategySettings settings
    ) {
        Map<Long, MaterialSupplierCandidate> assignments = portfolioAssignments(drafts, suppliers, settings);
        Set<Long> coreIds = Set.copyOf(settings.coreDemandItemIds());
        int coreCovered = (int) coreIds.stream().filter(assignments::containsKey).count();
        int score = assignments.values().stream().mapToInt(candidate -> weightedProductScore(candidate, settings)).sum();
        BigDecimal amount = assignments.entrySet().stream()
            .map(entry -> lineAmount(entry.getValue(), draftByItemId(drafts, entry.getKey()).pricingQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PortfolioScore(coreCovered, assignments.size(), score, amount);
    }

    private int comparePortfolio(PortfolioScore left, PortfolioScore right) {
        int core = Integer.compare(left.coreCovered(), right.coreCovered());
        if (core != 0) return core;
        int coverage = Integer.compare(left.covered(), right.covered());
        if (coverage != 0) return coverage;
        int score = Integer.compare(left.objectiveScore(), right.objectiveScore());
        if (score != 0) return score;
        return right.amount().compareTo(left.amount());
    }

    private Map<Long, MaterialSupplierCandidate> portfolioAssignments(
        List<ComparisonItemDraft> drafts,
        List<SupplierKey> suppliers,
        MaterialComparisonStrategySettings settings
    ) {
        if (suppliers.isEmpty()) return new java.util.LinkedHashMap<>();
        Set<SupplierKey> allowed = Set.copyOf(suppliers);
        Map<Long, MaterialSupplierCandidate> result = new java.util.LinkedHashMap<>();
        for (ComparisonItemDraft draft : drafts) {
            draft.candidates().stream()
                .filter(candidate -> strategyCandidateEligible(candidate, settings) && allowed.contains(SupplierKey.from(candidate)))
                .min(strategyCandidateComparator(settings))
                .ifPresent(candidate -> result.put(draft.item().itemId(), candidate));
        }
        return result;
    }

    private void ensureEachSelectedSupplierContributes(
        List<ComparisonItemDraft> drafts,
        List<SupplierKey> selected,
        Map<Long, MaterialSupplierCandidate> assignments,
        MaterialComparisonStrategySettings settings
    ) {
        for (SupplierKey supplier : selected) {
            boolean used = assignments.values().stream().anyMatch(candidate -> supplier.equals(SupplierKey.from(candidate)));
            if (used) continue;
            ComparisonItemDraft bestDraft = drafts.stream()
                .filter(draft -> draft.candidates().stream().anyMatch(candidate -> supplier.equals(SupplierKey.from(candidate))))
                .min(Comparator.comparingInt(draft -> replacementLoss(draft, supplier, assignments, settings)))
                .orElse(null);
            if (bestDraft == null) continue;
            bestDraft.candidates().stream()
                .filter(candidate -> supplier.equals(SupplierKey.from(candidate)) && strategyCandidateEligible(candidate, settings))
                .min(strategyCandidateComparator(settings))
                .ifPresent(candidate -> assignments.put(bestDraft.item().itemId(), candidate));
        }
    }

    private int replacementLoss(
        ComparisonItemDraft draft,
        SupplierKey supplier,
        Map<Long, MaterialSupplierCandidate> assignments,
        MaterialComparisonStrategySettings settings
    ) {
        int current = weightedProductScore(assignments.get(draft.item().itemId()), settings);
        int replacement = draft.candidates().stream()
            .filter(candidate -> supplier.equals(SupplierKey.from(candidate)))
            .mapToInt(candidate -> weightedProductScore(candidate, settings))
            .max().orElse(0);
        return current - replacement;
    }

    private ComparisonItemDraft draftByItemId(List<ComparisonItemDraft> drafts, Long itemId) {
        return drafts.stream().filter(draft -> java.util.Objects.equals(draft.item().itemId(), itemId)).findFirst().orElseThrow();
    }

    private boolean strategyCandidateEligible(
        MaterialSupplierCandidate candidate,
        MaterialComparisonStrategySettings settings
    ) {
        if (candidate == null || candidate.unitPrice() == null) {
            return false;
        }
        return settings.priceEnabled() || candidate.qualityScore() > 0;
    }

    private SingleSupplierSelection chooseSingleSupplier(
        List<ComparisonItemDraft> drafts,
        MaterialComparisonStrategySettings settings
    ) {
        Map<SupplierKey, List<SupplierItemPick>> picksBySupplier = drafts.stream()
            .flatMap(draft -> draft.candidates().stream()
                .filter(candidate -> candidate.unitPrice() != null)
                .map(candidate -> new SupplierItemPick(draft, candidate)))
            .collect(Collectors.groupingBy(
                pick -> SupplierKey.from(pick.candidate()),
                java.util.LinkedHashMap::new,
                Collectors.toList()
            ));

        List<SingleSupplierOption> options = picksBySupplier.entrySet().stream()
            .map(entry -> singleSupplierOption(entry.getKey(), entry.getValue(), drafts.size(), settings))
            .filter(option -> option.matchedCount() > 0)
            .toList();
        SingleSupplierOption priceBest = settings.priceEnabled()
            ? options.stream().min(this::compareSingleSupplierOption).orElse(null)
            : null;
        SingleSupplierOption qualityBest = settings.qualityEnabled()
            ? options.stream().filter(option -> option.qualityScore() > 0)
                .min(this::compareQualitySupplierOption).orElse(null)
            : null;
        SingleSupplierOption best = priceBest != null ? priceBest : qualityBest;
        if (best == null) {
            return new SingleSupplierSelection(
                new MaterialDemandComparisonStrategy("SINGLE_SUPPLIER", "集中采购", 0, drafts.size(), drafts.size(), 0, BigDecimal.ZERO, BigDecimal.ZERO, CNY, List.of(), true, null, objectiveTags(settings)),
                Map.of()
            );
        }
        Map<SupplierKey, MaterialDemandComparisonSupplier> alternatives = new java.util.LinkedHashMap<>();
        if (priceBest != null) alternatives.put(priceBest.key(), withSupplierTags(priceBest, List.of("价格最低")));
        if (qualityBest != null) alternatives.merge(
            qualityBest.key(),
            withSupplierTags(qualityBest, List.of("质量最高")),
            (left, right) -> new MaterialDemandComparisonSupplier(
                left.companyId(), left.supplierName(), left.matchedCount(), left.totalCount(), left.unmatchedCount(),
                left.unpricedCount(), left.stockSatisfiedCount(), left.totalAmount(), left.totalAmountUsd(), left.currency(),
                java.util.stream.Stream.concat(left.attributeTags().stream(), right.attributeTags().stream()).distinct().toList(),
                Math.max(left.coreItemCount(), right.coreItemCount()), Math.max(left.qualityScore(), right.qualityScore()),
                Math.max(left.priceScore(), right.priceScore())
            )
        );
        int coreRequired = settings.coreDemandItemIds().size();
        boolean coreComplete = best.coreItemCount() == coreRequired;
        return new SingleSupplierSelection(
            new MaterialDemandComparisonStrategy(
                "SINGLE_SUPPLIER",
                "集中采购",
                best.matchedCount(),
                drafts.size(),
                drafts.size() - best.matchedCount(),
                0,
                best.totalAmount(),
                best.totalAmountUsd(),
                CNY,
                List.copyOf(alternatives.values()),
                coreComplete,
                coreComplete ? null : "CORE_ITEM_UNCOVERED",
                objectiveTags(settings)
            ),
            best.candidateByItemId()
        );
    }

    private SingleSupplierOption singleSupplierOption(
        SupplierKey key,
        List<SupplierItemPick> picks,
        int totalCount,
        MaterialComparisonStrategySettings settings
    ) {
        Map<Long, SupplierItemPick> bestByItem = picks.stream()
            .collect(Collectors.toMap(
                pick -> pick.draft().item().itemId(),
                Function.identity(),
                (left, right) -> strategyCandidateComparator(settings).compare(left.candidate(), right.candidate()) <= 0 ? left : right
            ));
        List<SupplierItemPick> bestPicks = new ArrayList<>(bestByItem.values());
        int stockSatisfied = (int) bestPicks.stream()
            .filter(pick -> stockSatisfied(pick.candidate().stockQty(), pick.draft().pricingQuantity()))
            .count();
        BigDecimal totalAmount = bestPicks.stream()
            .map(pick -> lineAmount(pick.candidate(), pick.draft().pricingQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmountUsd = bestPicks.stream()
            .map(pick -> lineAmountUsd(pick.candidate(), pick.draft().pricingQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Long, MaterialSupplierCandidate> candidateByItemId = bestPicks.stream()
            .collect(Collectors.toMap(pick -> pick.draft().item().itemId(), SupplierItemPick::candidate));
        Set<Long> coreIds = Set.copyOf(settings.coreDemandItemIds());
        int coreItemCount = (int) candidateByItemId.keySet().stream().filter(coreIds::contains).count();
        int qualityScore = roundedAverage(bestPicks.stream().map(pick -> pick.candidate().qualityScore()).toList());
        int priceScore = roundedAverage(bestPicks.stream().map(pick -> pick.candidate().priceScore()).toList());
        MaterialDemandComparisonSupplier supplier = new MaterialDemandComparisonSupplier(
            key.companyId(),
            key.supplierName(),
            bestPicks.size(),
            totalCount,
            totalCount - bestPicks.size(),
            0,
            stockSatisfied,
            totalAmount,
            totalAmountUsd,
            CNY,
            List.of(),
            coreItemCount,
            qualityScore,
            priceScore
        );
        return new SingleSupplierOption(key, supplier, bestPicks.size(), stockSatisfied, totalAmount, totalAmountUsd, candidateByItemId, coreItemCount, qualityScore, priceScore);
    }

    private MaterialDemandComparisonSupplier supplierSummary(
        SupplierKey key,
        List<Map.Entry<Long, MaterialSupplierCandidate>> entries,
        List<ComparisonItemDraft> drafts,
        Set<Long> coreIds
    ) {
        int stockSatisfied = (int) entries.stream()
            .filter(entry -> stockSatisfied(entry.getValue().stockQty(), draftByItemId(drafts, entry.getKey()).pricingQuantity()))
            .count();
        int coreCount = (int) entries.stream().filter(entry -> coreIds.contains(entry.getKey())).count();
        List<MaterialSupplierCandidate> candidates = entries.stream().map(Map.Entry::getValue).toList();
        return new MaterialDemandComparisonSupplier(
            key.companyId(),
            key.supplierName(),
            entries.size(),
            drafts.size(),
            drafts.size() - entries.size(),
            0,
            stockSatisfied,
            entries.stream().map(entry -> lineAmount(entry.getValue(), draftByItemId(drafts, entry.getKey()).pricingQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add),
            entries.stream().map(entry -> lineAmountUsd(entry.getValue(), draftByItemId(drafts, entry.getKey()).pricingQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add),
            CNY,
            coreCount > 0 ? List.of("核心商品 × " + coreCount) : List.of(),
            coreCount,
            roundedAverage(candidates.stream().map(MaterialSupplierCandidate::qualityScore).toList()),
            roundedAverage(candidates.stream().map(MaterialSupplierCandidate::priceScore).toList())
        );
    }

    private MaterialDemandComparisonSupplier withSupplierTags(SingleSupplierOption option, List<String> tags) {
        MaterialDemandComparisonSupplier supplier = option.supplier();
        List<String> combined = new ArrayList<>(tags);
        if (option.coreItemCount() > 0) combined.add("核心商品 × " + option.coreItemCount());
        return new MaterialDemandComparisonSupplier(
            supplier.companyId(), supplier.supplierName(), supplier.matchedCount(), supplier.totalCount(),
            supplier.unmatchedCount(), supplier.unpricedCount(), supplier.stockSatisfiedCount(), supplier.totalAmount(),
            supplier.totalAmountUsd(), supplier.currency(), List.copyOf(combined), option.coreItemCount(),
            option.qualityScore(), option.priceScore()
        );
    }

    private List<String> objectiveTags(MaterialComparisonStrategySettings settings) {
        List<String> result = new ArrayList<>();
        if (settings.priceEnabled()) result.add("价格低 " + settings.priceLevel() + "档");
        if (settings.qualityEnabled()) result.add("质量高 " + settings.qualityLevel() + "档");
        return List.copyOf(result);
    }

    private int roundedAverage(List<Integer> values) {
        return values.isEmpty() ? 0 : (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private BigDecimal amount(List<ComparisonItemDraft> drafts, Function<ComparisonItemDraft, MaterialSupplierCandidate> candidateAccessor) {
        return drafts.stream()
            .map(draft -> lineAmount(candidateAccessor.apply(draft), draft.pricingQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal amountUsd(List<ComparisonItemDraft> drafts, Function<ComparisonItemDraft, MaterialSupplierCandidate> candidateAccessor) {
        return drafts.stream()
            .map(draft -> lineAmountUsd(candidateAccessor.apply(draft), draft.pricingQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal lineAmount(MaterialSupplierCandidate candidate, BigDecimal quantity) {
        if (candidate == null || candidate.unitPrice() == null) {
            return BigDecimal.ZERO;
        }
        return candidate.unitPrice().multiply(quantity);
    }

    private BigDecimal lineAmountUsd(MaterialSupplierCandidate candidate, BigDecimal quantity) {
        if (candidate == null || candidate.unitPriceUsd() == null) {
            return BigDecimal.ZERO;
        }
        return candidate.unitPriceUsd().multiply(quantity);
    }

    private MaterialSupplierCandidate pricedCandidate(MaterialSupplierCandidate candidate, BigDecimal requestedQty) {
        List<MaterialSupplierUnitPriceOption> options = candidate.unitPriceOptions();
        if (options == null || options.isEmpty()) {
            String unit = firstNonBlank(candidate.selectedUnit(), candidate.stockUnit());
            options = candidate.unitPrice() == null
                ? List.of()
                : List.of(new MaterialSupplierUnitPriceOption(unit, candidate.unitPrice(), usd(candidate.unitPrice()), true));
        }
        return candidate.withUnitPriceOptions(options).withLineAmounts(requestedQty);
    }

    private BigDecimal usd(BigDecimal cnyAmount) {
        return cnyAmount == null ? null : cnyAmount.divide(USD_RATE, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal parsePositiveQuantity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal quantity = new BigDecimal(value.replaceAll("[^0-9.\\-]", ""));
            return quantity.compareTo(BigDecimal.ZERO) > 0 ? quantity : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal price(BigDecimal value) {
        return value == null ? MISSING_PRICE : value;
    }

    private boolean stockSatisfied(BigDecimal stockQty, BigDecimal requestedQty) {
        return stockQty != null && requestedQty != null && stockQty.compareTo(requestedQty) >= 0;
    }

    private String preferredCategory(MaterialDemandItemResponse item) {
        List<MaterialMatchCandidate> candidates = item.candidateSnapshot() == null ? item.candidates() : item.candidateSnapshot();
        if (candidates == null) {
            return null;
        }
        return candidates.stream()
            .map(MaterialMatchCandidate::categoryCode)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(null);
    }

    private double coverage(List<String> sourceTokens, List<String> candidateTokens) {
        if (sourceTokens.isEmpty() || candidateTokens.isEmpty()) {
            return 0;
        }
        Set<String> candidateSet = new LinkedHashSet<>(candidateTokens);
        long overlap = sourceTokens.stream().filter(candidateSet::contains).count();
        return (double) overlap / sourceTokens.size();
    }

    private static List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : TOKEN_SPLITTER.split(value.toUpperCase(Locale.ROOT))) {
            if (token.isBlank() || STOP_WORDS.contains(token) || token.chars().allMatch(Character::isDigit)) {
                continue;
            }
            boolean containsHan = token.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
            if (!containsHan && token.length() < 3) {
                continue;
            }
            tokens.add(token);
            if (containsHan && token.length() > 2) {
                for (int index = 0; index < token.length() - 1; index++) {
                    tokens.add(token.substring(index, index + 2));
                }
            }
        }
        return tokens.stream().distinct().toList();
    }

    private List<String> searchTerms(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.length() <= 80) {
            terms.add(trimmed);
        }
        terms.addAll(tokenize(trimmed));
        return List.copyOf(terms);
    }

    private List<String> mergeTokens(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged.stream().distinct().toList();
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String datePart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        return text.length() >= 10 ? text.substring(0, 10) : text;
    }

    private int compareSingleSupplierOption(SingleSupplierOption left, SingleSupplierOption right) {
        int core = Integer.compare(right.coreItemCount(), left.coreItemCount());
        if (core != 0) {
            return core;
        }
        int matched = Integer.compare(right.matchedCount(), left.matchedCount());
        if (matched != 0) {
            return matched;
        }
        int amount = left.totalAmount().compareTo(right.totalAmount());
        if (amount != 0) {
            return amount;
        }
        int stock = Integer.compare(right.stockSatisfiedCount(), left.stockSatisfiedCount());
        if (stock != 0) {
            return stock;
        }
        int supplier = nullToEmpty(left.key().supplierName()).compareTo(nullToEmpty(right.key().supplierName()));
        if (supplier != 0) {
            return supplier;
        }
        return Long.compare(
            left.key().companyId() == null ? Long.MAX_VALUE : left.key().companyId(),
            right.key().companyId() == null ? Long.MAX_VALUE : right.key().companyId()
        );
    }

    private int compareQualitySupplierOption(SingleSupplierOption left, SingleSupplierOption right) {
        int core = Integer.compare(right.coreItemCount(), left.coreItemCount());
        if (core != 0) {
            return core;
        }
        int matched = Integer.compare(right.matchedCount(), left.matchedCount());
        if (matched != 0) {
            return matched;
        }
        int quality = Integer.compare(right.qualityScore(), left.qualityScore());
        if (quality != 0) {
            return quality;
        }
        return compareSingleSupplierOption(left, right);
    }

    private record ComparisonItemDraft(
        MaterialDemandItemResponse item,
        BigDecimal pricingQuantity,
        String pricingQuantityNote,
        List<MaterialSupplierCandidate> candidates,
        MaterialSupplierCandidate lowestCandidate,
        String emptyReason,
        String aiStatus
    ) {
        MaterialDemandComparisonItem toResponse(
            MaterialSupplierCandidate mixedSupplierCandidate,
            MaterialSupplierCandidate singleSupplierCandidate
        ) {
            return new MaterialDemandComparisonItem(
                item.itemId(),
                item.rowNo(),
                item.impaCode(),
                item.platformCode(),
                item.productName(),
                item.description(),
                item.specification(),
                item.quantity(),
                pricingQuantity,
                pricingQuantityNote,
                item.unit(),
                item.remarks(),
                firstNonBlank(item.supplierItemNo(), item.impaCode(), item.platformCode()),
                firstNonBlank(item.rawNameSpec(), item.description(), item.productName()),
                mixedSupplierCandidate == null ? lowestCandidate : mixedSupplierCandidate,
                singleSupplierCandidate,
                candidates,
                emptyReason,
                item.actualQuotePrice(),
                item.actualQuoteCurrency(),
                item.quoteMarkupPercent(),
                item.quoteSupplierSkuId(),
                item.quoteSelectedUnit(),
                item.quoteUnitPrice(),
                item.quoteUnitPriceUsd(),
                item.quoteStrategyType()
            );
        }
    }

    private record SupplierKey(Long companyId, String supplierName) {
        static SupplierKey from(MaterialSupplierCandidate candidate) {
            return new SupplierKey(candidate.companyId(), candidate.supplierName());
        }
    }

    private record ScoredSupplierCandidate(
        MaterialSupplierCandidate candidate,
        int rank,
        BigDecimal price,
        boolean stockSatisfied
    ) {
    }

    private record SupplierItemPick(
        ComparisonItemDraft draft,
        MaterialSupplierCandidate candidate
    ) {
    }

    private record SingleSupplierOption(
        SupplierKey key,
        MaterialDemandComparisonSupplier supplier,
        int matchedCount,
        int stockSatisfiedCount,
        BigDecimal totalAmount,
        BigDecimal totalAmountUsd,
        Map<Long, MaterialSupplierCandidate> candidateByItemId,
        int coreItemCount,
        int qualityScore,
        int priceScore
    ) {
    }

    private record SingleSupplierSelection(
        MaterialDemandComparisonStrategy strategy,
        Map<Long, MaterialSupplierCandidate> candidateByItemId
    ) {
    }

    private record MixedSupplierSelection(
        MaterialDemandComparisonStrategy strategy,
        Map<Long, MaterialSupplierCandidate> candidateByItemId
    ) {
    }

    private record PortfolioScore(
        int coreCovered,
        int covered,
        int objectiveScore,
        BigDecimal amount
    ) {
    }

    private boolean trustedCodeStatus(String codeStatus) {
        String normalized = nullToEmpty(codeStatus).toUpperCase(Locale.ROOT);
        return "CODE_MATCHED".equals(normalized) || "SPEC_MATCHED".equals(normalized) || "MATCHED".equals(normalized);
    }

    private boolean hasComparableSpecification(List<String> tokens) {
        return tokens.stream().anyMatch(token -> token.chars().anyMatch(Character::isDigit));
    }

    private record IndexedSupplierCandidate(
        MaterialSupplierCandidate candidate,
        List<String> nameTokens,
        List<String> specTokens
    ) {
    }

    private record SupplierCandidateIndex(
        List<IndexedSupplierCandidate> all,
        Map<String, List<IndexedSupplierCandidate>> byCode,
        Map<String, List<IndexedSupplierCandidate>> bySupplierSku,
        Map<String, List<IndexedSupplierCandidate>> byNameToken,
        Map<String, List<IndexedSupplierCandidate>> byCategory
    ) {
        static SupplierCandidateIndex from(List<MaterialSupplierCandidate> candidates) {
            List<IndexedSupplierCandidate> prepared = prepare(candidates);
            return new SupplierCandidateIndex(
                prepared,
                indexByCode(prepared),
                prepared.stream()
                    .filter(candidate -> !normalizeCandidateCode(candidate.candidate().supplierSkuCode()).isBlank())
                    .collect(Collectors.groupingBy(
                        candidate -> normalizeCandidateCode(candidate.candidate().supplierSkuCode()),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                    )),
                indexByNameToken(prepared),
                prepared.stream()
                    .filter(candidate -> candidate.candidate().categoryCode() != null && !candidate.candidate().categoryCode().isBlank())
                    .collect(Collectors.groupingBy(
                        candidate -> candidate.candidate().categoryCode(),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                    ))
            );
        }

        List<IndexedSupplierCandidate> indexedCandidates(Set<String> platformCodes, String supplierSku) {
            LinkedHashSet<IndexedSupplierCandidate> matches = new LinkedHashSet<>();
            platformCodes.forEach(code -> matches.addAll(byCode.getOrDefault(code, List.of())));
            if (!supplierSku.isBlank()) {
                matches.addAll(bySupplierSku.getOrDefault(supplierSku, List.of()));
            }
            return List.copyOf(matches);
        }

        List<IndexedSupplierCandidate> candidatesForEvidence(
            Set<String> platformCodes,
            String supplierSku,
            List<String> nameTokens,
            String preferredCategory
        ) {
            LinkedHashSet<IndexedSupplierCandidate> matches = new LinkedHashSet<>(indexedCandidates(platformCodes, supplierSku));
            nameTokens.forEach(token -> matches.addAll(byNameToken.getOrDefault(token, List.of())));
            if (matches.isEmpty() && preferredCategory != null && !preferredCategory.isBlank()) {
                matches.addAll(byCategory.getOrDefault(preferredCategory, List.of()));
            }
            return matches.isEmpty() ? all : List.copyOf(matches);
        }

        List<IndexedSupplierCandidate> fallbackCandidates(List<String> nameTokens, String preferredCategory) {
            LinkedHashSet<IndexedSupplierCandidate> matches = new LinkedHashSet<>();
            nameTokens.forEach(token -> matches.addAll(byNameToken.getOrDefault(token, List.of())));
            if (matches.isEmpty() && preferredCategory != null && !preferredCategory.isBlank()) {
                matches.addAll(byCategory.getOrDefault(preferredCategory, List.of()));
            }
            return matches.isEmpty() ? all : List.copyOf(matches);
        }

        private static List<IndexedSupplierCandidate> prepare(List<MaterialSupplierCandidate> candidates) {
            return candidates.stream()
                .map(candidate -> new IndexedSupplierCandidate(
                    candidate,
                    tokenizeCandidate(candidate.productName()),
                    mergeCandidateTokens(tokenizeCandidate(candidate.attributeSummary()), tokenizeCandidate(candidate.packageSpec()))
                ))
                .toList();
        }

        private static Map<String, List<IndexedSupplierCandidate>> indexByCode(List<IndexedSupplierCandidate> candidates) {
            Map<String, List<IndexedSupplierCandidate>> index = new java.util.LinkedHashMap<>();
            candidates.forEach(candidate -> {
                addIndexCandidate(index, normalizeCandidateCode(candidate.candidate().impaCode()), candidate);
                addIndexCandidate(index, normalizeCandidateCode(candidate.candidate().platformCode()), candidate);
            });
            return index;
        }

        private static Map<String, List<IndexedSupplierCandidate>> indexByNameToken(List<IndexedSupplierCandidate> candidates) {
            Map<String, List<IndexedSupplierCandidate>> index = new java.util.LinkedHashMap<>();
            candidates.forEach(candidate -> candidate.nameTokens().forEach(token -> addIndexCandidate(index, token, candidate)));
            return index;
        }

        private static void addIndexCandidate(Map<String, List<IndexedSupplierCandidate>> index, String code, IndexedSupplierCandidate candidate) {
            if (code.isBlank()) return;
            index.computeIfAbsent(code, ignored -> new ArrayList<>()).add(candidate);
        }

        private static String normalizeCandidateCode(String value) {
            if (value == null) {
                return "";
            }
            return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        }

        private static List<String> tokenizeCandidate(String value) {
            return tokenize(value);
        }

        private static List<String> mergeCandidateTokens(List<String> first, List<String> second) {
            List<String> merged = new ArrayList<>(first);
            merged.addAll(second);
            return merged.stream().distinct().toList();
        }
    }
}
