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
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^A-Za-z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of(
        "AND", "THE", "FOR", "WITH", "WITHOUT", "OF", "IN", "ON", "A", "AN",
        "NO", "TYPE", "PCS", "PC", "SET", "BOX", "CTN", "INNER", "OUTER",
        "CM", "MM", "KG", "KGS", "RMB", "FOB", "WAREHOUSE", "MODEL", "SIZE"
    );

    private final CurrentUserService currentUserService;
    private final MaterialDemandRepository materialDemandRepository;
    private final MaterialSupplierCandidateProvider supplierCandidateProvider;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public MaterialDemandComparisonService(
        CurrentUserService currentUserService,
        MaterialDemandRepository materialDemandRepository,
        MaterialSupplierCandidateProvider supplierCandidateProvider,
        PurchaseOrderRepository purchaseOrderRepository
    ) {
        this.currentUserService = currentUserService;
        this.materialDemandRepository = materialDemandRepository;
        this.supplierCandidateProvider = supplierCandidateProvider;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public MaterialDemandComparisonResponse comparison(String authorizationHeader, Long demandId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSummaryResponse demand = requireDemand(currentUser.companyId(), demandId);
        if ("DISCARDED".equalsIgnoreCase(demand.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MATERIAL_DEMAND_DISCARDED");
        }
        materialDemandRepository.markComparingIfSaved(currentUser.companyId(), demandId);
        List<MaterialDemandItemResponse> demandItems = materialDemandRepository.items(currentUser.companyId(), demandId);
        SupplierCandidateIndex supplierPool = SupplierCandidateIndex.from(onShelfSupplierPool());
        Long existingPurchaseOrderId = purchaseOrderRepository.findFirstActiveOrderIdByDemand(currentUser.companyId(), demandId);
        if (existingPurchaseOrderId != null && existingPurchaseOrderId <= 0) {
            existingPurchaseOrderId = null;
        }

        List<ComparisonItemDraft> drafts = demandItems.stream()
            .map(item -> draftItem(item, supplierPool))
            .toList();
        SingleSupplierSelection singleSupplier = chooseSingleSupplier(drafts);
        List<MaterialDemandComparisonItem> items = drafts.stream()
            .map(draft -> draft.toResponse(singleSupplier.candidateByItemId().get(draft.item().itemId())))
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
            List.of(lowestMixedStrategy(drafts), singleSupplier.strategy()),
            items,
            existingPurchaseOrderId != null || "ORDERED".equalsIgnoreCase(demand.status()),
            "DISCARDED".equalsIgnoreCase(demand.status()),
            existingPurchaseOrderId
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
        return draftItem(item, SupplierCandidateIndex.from(onShelfSupplierPool())).candidates();
    }

    private List<MaterialSupplierCandidate> onShelfSupplierPool() {
        return supplierCandidateProvider.findOnShelfCandidates().stream()
            .filter(candidate -> "ON_SHELF".equalsIgnoreCase(nullToEmpty(candidate.shelfStatus())))
            .toList();
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
        SupplierCandidateIndex supplierPool
    ) {
        BigDecimal pricingQuantity = parsePositiveQuantity(item.quantity());
        String pricingQuantityNote = pricingQuantity == null ? "璁′环鏁伴噺鎸?1" : null;
        BigDecimal safePricingQuantity = pricingQuantity == null ? BigDecimal.ONE : pricingQuantity;
        List<MaterialSupplierCandidate> candidates = supplierCandidates(item, supplierPool, safePricingQuantity);
        MaterialSupplierCandidate lowest = candidates.stream()
            .filter(candidate -> candidate.unitPrice() != null)
            .min(Comparator
                .comparing(MaterialSupplierCandidate::unitPrice)
                .thenComparing(MaterialSupplierCandidate::skuId, Comparator.nullsLast(Long::compareTo)))
            .orElse(null);
        String emptyReason = null;
        if (candidates.isEmpty()) {
            emptyReason = "NO_SUPPLIER_CANDIDATE";
        } else if (lowest == null) {
            emptyReason = "NO_PRICED_CANDIDATE";
        }
        return new ComparisonItemDraft(item, safePricingQuantity, pricingQuantityNote, candidates, lowest, emptyReason);
    }

    private List<MaterialSupplierCandidate> supplierCandidates(
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

        List<IndexedSupplierCandidate> indexedCandidates = supplierPool.indexedCandidates(platformCodes, supplierSku);
        List<IndexedSupplierCandidate> candidatesToScore = indexedCandidates.isEmpty() ? supplierPool.fallbackCandidates(nameTokens, preferredCategory) : indexedCandidates;

        return candidatesToScore.stream()
            .map(candidate -> scoreCandidate(candidate, supplierSku, platformCodes, nameTokens, specTokens, preferredCategory, requestedQty))
            .filter(candidate -> candidate.rank() > 0)
            .sorted(candidateComparator())
            .limit(MAX_CANDIDATES_PER_ITEM)
            .map(candidate -> pricedCandidate(candidate.candidate(), requestedQty))
            .toList();
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
        boolean codeMatch = platformCodes.stream()
            .anyMatch(code -> code.equals(normalizeCode(candidate.impaCode())) || code.equals(normalizeCode(candidate.platformCode())));
        boolean supplierSkuMatch = !supplierSku.isBlank() && supplierSku.equals(normalizeCode(candidate.supplierSkuCode()));
        double nameScore = coverage(nameTokens, indexedCandidate.nameTokens());
        double specScore = coverage(specTokens, indexedCandidate.specTokens());
        boolean nameSpecMatch = nameScore >= 0.6 || (!nameTokens.isEmpty() && nameScore > 0 && (specTokens.isEmpty() || specScore > 0));
        boolean categoryMatch = preferredCategory != null && preferredCategory.equals(candidate.categoryCode());

        int rank = 0;
        String matchType = null;
        String reason = null;
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

    private MaterialDemandComparisonStrategy lowestMixedStrategy(List<ComparisonItemDraft> drafts) {
        List<ComparisonItemDraft> priced = drafts.stream()
            .filter(draft -> draft.lowestCandidate() != null)
            .toList();
        List<ComparisonItemDraft> unpriced = drafts.stream()
            .filter(draft -> !draft.candidates().isEmpty() && draft.lowestCandidate() == null)
            .toList();
        Map<SupplierKey, List<ComparisonItemDraft>> bySupplier = priced.stream()
            .collect(Collectors.groupingBy(
                draft -> SupplierKey.from(draft.lowestCandidate()),
                java.util.LinkedHashMap::new,
                Collectors.toList()
            ));
        List<MaterialDemandComparisonSupplier> suppliers = bySupplier.entrySet().stream()
            .map(entry -> supplierSummary(entry.getKey(), entry.getValue(), drafts.size()))
            .sorted(Comparator
                .comparing(MaterialDemandComparisonSupplier::totalAmount)
                .thenComparing(MaterialDemandComparisonSupplier::supplierName, Comparator.nullsLast(String::compareTo))
                .thenComparing(MaterialDemandComparisonSupplier::companyId, Comparator.nullsLast(Long::compareTo)))
            .toList();
        boolean enabled = suppliers.size() > 1;
        return new MaterialDemandComparisonStrategy(
            "LOWEST_MIXED",
            "最低混供",
            priced.size(),
            drafts.size(),
            drafts.size() - priced.size(),
            unpriced.size(),
            amount(priced, ComparisonItemDraft::lowestCandidate),
            amountUsd(priced, ComparisonItemDraft::lowestCandidate),
            CNY,
            suppliers,
            enabled,
            enabled ? null : "ONLY_ONE_SUPPLIER"
        );
    }

    private SingleSupplierSelection chooseSingleSupplier(List<ComparisonItemDraft> drafts) {
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
            .map(entry -> singleSupplierOption(entry.getKey(), entry.getValue(), drafts.size()))
            .filter(option -> option.matchedCount() > 0)
            .toList();
        SingleSupplierOption best = options.stream()
            .min(this::compareSingleSupplierOption)
            .orElse(null);
        if (best == null) {
            return new SingleSupplierSelection(
                new MaterialDemandComparisonStrategy("SINGLE_SUPPLIER", "集中采购", 0, drafts.size(), drafts.size(), 0, BigDecimal.ZERO, BigDecimal.ZERO, CNY, List.of(), true, null),
                Map.of()
            );
        }
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
                List.of(best.supplier()),
                true,
                null
            ),
            best.candidateByItemId()
        );
    }

    private SingleSupplierOption singleSupplierOption(SupplierKey key, List<SupplierItemPick> picks, int totalCount) {
        Map<Long, SupplierItemPick> bestByItem = picks.stream()
            .collect(Collectors.toMap(
                pick -> pick.draft().item().itemId(),
                Function.identity(),
                (left, right) -> left.candidate().unitPrice().compareTo(right.candidate().unitPrice()) <= 0 ? left : right
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
            CNY
        );
        return new SingleSupplierOption(key, supplier, bestPicks.size(), stockSatisfied, totalAmount, totalAmountUsd, candidateByItemId);
    }

    private MaterialDemandComparisonSupplier supplierSummary(SupplierKey key, List<ComparisonItemDraft> drafts, int totalCount) {
        int stockSatisfied = (int) drafts.stream()
            .filter(draft -> stockSatisfied(draft.lowestCandidate().stockQty(), draft.pricingQuantity()))
            .count();
        return new MaterialDemandComparisonSupplier(
            key.companyId(),
            key.supplierName(),
            drafts.size(),
            totalCount,
            totalCount - drafts.size(),
            0,
            stockSatisfied,
            amount(drafts, ComparisonItemDraft::lowestCandidate),
            amountUsd(drafts, ComparisonItemDraft::lowestCandidate),
            CNY
        );
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

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : TOKEN_SPLITTER.split(value.toUpperCase(Locale.ROOT))) {
            if (token.length() < 3 || STOP_WORDS.contains(token) || token.chars().allMatch(Character::isDigit)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens.stream().distinct().toList();
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

    private record ComparisonItemDraft(
        MaterialDemandItemResponse item,
        BigDecimal pricingQuantity,
        String pricingQuantityNote,
        List<MaterialSupplierCandidate> candidates,
        MaterialSupplierCandidate lowestCandidate,
        String emptyReason
    ) {
        MaterialDemandComparisonItem toResponse(MaterialSupplierCandidate singleSupplierCandidate) {
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
                lowestCandidate,
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
        Map<Long, MaterialSupplierCandidate> candidateByItemId
    ) {
    }

    private record SingleSupplierSelection(
        MaterialDemandComparisonStrategy strategy,
        Map<Long, MaterialSupplierCandidate> candidateByItemId
    ) {
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
            if (value == null || value.isBlank()) {
                return List.of();
            }
            List<String> tokens = new ArrayList<>();
            for (String token : TOKEN_SPLITTER.split(value.toUpperCase(Locale.ROOT))) {
                if (token.length() < 3 || STOP_WORDS.contains(token) || token.chars().allMatch(Character::isDigit)) {
                    continue;
                }
                tokens.add(token);
            }
            return tokens.stream().distinct().toList();
        }

        private static List<String> mergeCandidateTokens(List<String> first, List<String> second) {
            List<String> merged = new ArrayList<>(first);
            merged.addAll(second);
            return merged.stream().distinct().toList();
        }
    }
}
