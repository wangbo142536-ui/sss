package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.util.List;

record MaterialDemandComparisonResponse(
    MaterialDemandSummaryResponse demand,
    MaterialDemandSupplyInfo supplyInfo,
    List<MaterialDemandComparisonStrategy> strategies,
    List<MaterialDemandComparisonItem> items,
    boolean isOrdered,
    boolean isDiscarded,
    Long existingPurchaseOrderId,
    MaterialComparisonAiSummary aiProcessing,
    MaterialComparisonStrategySettings strategySettings
) {
    MaterialDemandComparisonResponse(
        MaterialDemandSummaryResponse demand,
        MaterialDemandSupplyInfo supplyInfo,
        List<MaterialDemandComparisonStrategy> strategies,
        List<MaterialDemandComparisonItem> items,
        boolean isOrdered,
        boolean isDiscarded,
        Long existingPurchaseOrderId
    ) {
        this(demand, supplyInfo, strategies, items, isOrdered, isDiscarded, existingPurchaseOrderId,
            new MaterialComparisonAiSummary("DETERMINISTIC", 0, 0), MaterialComparisonStrategySettings.defaults());
    }

    MaterialDemandComparisonResponse(
        MaterialDemandSummaryResponse demand,
        MaterialDemandSupplyInfo supplyInfo,
        List<MaterialDemandComparisonStrategy> strategies,
        List<MaterialDemandComparisonItem> items,
        boolean isOrdered,
        boolean isDiscarded,
        Long existingPurchaseOrderId,
        MaterialComparisonAiSummary aiProcessing
    ) {
        this(demand, supplyInfo, strategies, items, isOrdered, isDiscarded, existingPurchaseOrderId,
            aiProcessing, MaterialComparisonStrategySettings.defaults());
    }
}

record MaterialComparisonAiSummary(
    String status,
    int appliedItemCount,
    int fallbackItemCount
) {
}

record MaterialComparisonStrategySettings(
    int mixedSupplierCount,
    boolean priceEnabled,
    int priceLevel,
    boolean qualityEnabled,
    int qualityLevel,
    List<Long> coreDemandItemIds,
    int strategyVersion
) {
    static MaterialComparisonStrategySettings defaults() {
        return new MaterialComparisonStrategySettings(3, true, 5, true, 3, List.of(), 1);
    }
}

record MaterialComparisonStrategySettingsRequest(
    Integer mixedSupplierCount,
    Boolean priceEnabled,
    Integer priceLevel,
    Boolean qualityEnabled,
    Integer qualityLevel,
    List<Long> coreDemandItemIds
) {
}

record MaterialDemandSupplyInfo(
    String supplyVessel,
    String supplyPort,
    String supplyPortCode,
    String supplyPortName,
    String vesselEta,
    String supplyDate,
    String weatherText,
    String weatherSource
) {
}

record MaterialDemandComparisonStrategy(
    String strategyType,
    String strategyName,
    int matchedCount,
    int totalCount,
    int unmatchedCount,
    int unpricedCount,
    BigDecimal totalAmount,
    BigDecimal totalAmountUsd,
    String currency,
    List<MaterialDemandComparisonSupplier> suppliers,
    boolean enabled,
    String disabledReason,
    List<String> attributeTags
) {
    MaterialDemandComparisonStrategy(
        String strategyType,
        String strategyName,
        int matchedCount,
        int totalCount,
        int unmatchedCount,
        int unpricedCount,
        BigDecimal totalAmount,
        String currency,
        List<MaterialDemandComparisonSupplier> suppliers,
        boolean enabled,
        String disabledReason
    ) {
        this(strategyType, strategyName, matchedCount, totalCount, unmatchedCount, unpricedCount, totalAmount, null, currency, suppliers, enabled, disabledReason, List.of());
    }


    MaterialDemandComparisonStrategy(
        String strategyType,
        String strategyName,
        int matchedCount,
        int totalCount,
        int unmatchedCount,
        int unpricedCount,
        BigDecimal totalAmount,
        BigDecimal totalAmountUsd,
        String currency,
        List<MaterialDemandComparisonSupplier> suppliers,
        boolean enabled,
        String disabledReason
    ) {
        this(strategyType, strategyName, matchedCount, totalCount, unmatchedCount, unpricedCount, totalAmount, totalAmountUsd, currency, suppliers, enabled, disabledReason, List.of());
    }
}

record MaterialDemandComparisonSupplier(
    Long companyId,
    String supplierName,
    int matchedCount,
    int totalCount,
    int unmatchedCount,
    int unpricedCount,
    int stockSatisfiedCount,
    BigDecimal totalAmount,
    BigDecimal totalAmountUsd,
    String currency,
    List<String> attributeTags,
    int coreItemCount,
    int qualityScore,
    int priceScore
) {
    MaterialDemandComparisonSupplier(
        Long companyId,
        String supplierName,
        int matchedCount,
        int totalCount,
        int unmatchedCount,
        int unpricedCount,
        int stockSatisfiedCount,
        BigDecimal totalAmount,
        String currency
    ) {
        this(companyId, supplierName, matchedCount, totalCount, unmatchedCount, unpricedCount, stockSatisfiedCount, totalAmount, null, currency, List.of(), 0, 0, 0);
    }

    MaterialDemandComparisonSupplier(
        Long companyId,
        String supplierName,
        int matchedCount,
        int totalCount,
        int unmatchedCount,
        int unpricedCount,
        int stockSatisfiedCount,
        BigDecimal totalAmount,
        BigDecimal totalAmountUsd,
        String currency
    ) {
        this(companyId, supplierName, matchedCount, totalCount, unmatchedCount, unpricedCount, stockSatisfiedCount, totalAmount, totalAmountUsd, currency, List.of(), 0, 0, 0);
    }
}

record MaterialDemandComparisonItem(
    Long demandItemId,
    Integer rowNo,
    String impaCode,
    String platformCode,
    String productName,
    String description,
    String specification,
    String quantity,
    BigDecimal pricingQuantity,
    String pricingQuantityNote,
    String unit,
    String remarks,
    String sourceSkuCode,
    String sourceSkuName,
    MaterialSupplierCandidate lowestCandidate,
    MaterialSupplierCandidate singleSupplierCandidate,
    List<MaterialSupplierCandidate> candidates,
    String emptyReason,
    BigDecimal actualQuotePrice,
    String actualQuoteCurrency,
    BigDecimal quoteMarkupPercent,
    Long quoteSupplierSkuId,
    String quoteSelectedUnit,
    BigDecimal quoteUnitPrice,
    BigDecimal quoteUnitPriceUsd,
    String quoteStrategyType
) {
    MaterialDemandComparisonItem(
        Long demandItemId,
        Integer rowNo,
        String impaCode,
        String platformCode,
        String productName,
        String description,
        String specification,
        String quantity,
        BigDecimal pricingQuantity,
        String pricingQuantityNote,
        String unit,
        String remarks,
        String sourceSkuCode,
        String sourceSkuName,
        MaterialSupplierCandidate lowestCandidate,
        MaterialSupplierCandidate singleSupplierCandidate,
        List<MaterialSupplierCandidate> candidates,
        String emptyReason
    ) {
        this(
            demandItemId,
            rowNo,
            impaCode,
            platformCode,
            productName,
            description,
            specification,
            quantity,
            pricingQuantity,
            pricingQuantityNote,
            unit,
            remarks,
            sourceSkuCode,
            sourceSkuName,
            lowestCandidate,
            singleSupplierCandidate,
            candidates,
            emptyReason,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    MaterialDemandComparisonItem(
        Long demandItemId,
        Integer rowNo,
        String impaCode,
        String platformCode,
        String productName,
        String description,
        String specification,
        String quantity,
        BigDecimal pricingQuantity,
        String pricingQuantityNote,
        String unit,
        String sourceSkuCode,
        String sourceSkuName,
        MaterialSupplierCandidate lowestCandidate,
        MaterialSupplierCandidate singleSupplierCandidate,
        List<MaterialSupplierCandidate> candidates,
        String emptyReason
    ) {
        this(
            demandItemId,
            rowNo,
            impaCode,
            platformCode,
            productName,
            description,
            specification,
            quantity,
            pricingQuantity,
            pricingQuantityNote,
            unit,
            null,
            sourceSkuCode,
            sourceSkuName,
            lowestCandidate,
            singleSupplierCandidate,
            candidates,
            emptyReason
        );
    }
}
