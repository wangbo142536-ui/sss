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
    Long existingPurchaseOrderId
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
    String disabledReason
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
        this(strategyType, strategyName, matchedCount, totalCount, unmatchedCount, unpricedCount, totalAmount, null, currency, suppliers, enabled, disabledReason);
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
    String currency
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
        this(companyId, supplierName, matchedCount, totalCount, unmatchedCount, unpricedCount, stockSatisfiedCount, totalAmount, null, currency);
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
    String sourceSkuCode,
    String sourceSkuName,
    MaterialSupplierCandidate lowestCandidate,
    MaterialSupplierCandidate singleSupplierCandidate,
    List<MaterialSupplierCandidate> candidates,
    String emptyReason
) {
}
