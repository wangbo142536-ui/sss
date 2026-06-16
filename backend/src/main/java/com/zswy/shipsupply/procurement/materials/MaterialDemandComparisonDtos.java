package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.util.List;

record MaterialDemandComparisonResponse(
    MaterialDemandSummaryResponse demand,
    MaterialDemandSupplyInfo supplyInfo,
    List<MaterialDemandComparisonStrategy> strategies,
    List<MaterialDemandComparisonItem> items
) {
}

record MaterialDemandSupplyInfo(
    String supplyVessel,
    String supplyPort,
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
    String currency,
    List<MaterialDemandComparisonSupplier> suppliers
) {
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
    String currency
) {
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
    MaterialSupplierCandidate lowestCandidate,
    MaterialSupplierCandidate singleSupplierCandidate,
    List<MaterialSupplierCandidate> candidates,
    String emptyReason
) {
}
