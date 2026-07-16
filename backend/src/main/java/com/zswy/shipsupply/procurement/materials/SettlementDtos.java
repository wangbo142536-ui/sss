package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.util.List;

record SettlementBatchRequest(
    Long purchaseOrderId,
    List<SettlementSourceRequest> items
) {
}

record SettlementSourceRequest(
    String sourceType,
    Long sourceId,
    BigDecimal quotedAmount,
    BigDecimal actualAmount
) {
}

record SettlementAmountUpdateRequest(
    BigDecimal actualAmount
) {
}

record SettlementInvoiceSubmitRequest(
    BigDecimal actualAmount,
    List<FileSnapshot> invoiceAttachments
) {
}

record SettlementOrderResponse(
    Long id,
    String settlementNo,
    Long purchaseOrderId,
    String purchaseOrderNo,
    String vesselName,
    Long supplierOrderId,
    Long trafficServiceOrderId,
    Long buyerCompanyId,
    String buyerCompanyName,
    Long providerCompanyId,
    String providerName,
    String settlementType,
    String materialType,
    BigDecimal quotedAmount,
    BigDecimal actualAmount,
    String currency,
    String status,
    List<FileSnapshot> invoiceAttachments,
    String invoiceSubmittedAt,
    String settledAt,
    Long createdBy,
    String createdAt,
    String updatedAt
) {
}

record SettlementOrderListResponse(
    List<SettlementOrderResponse> items,
    int page,
    int size,
    long total
) {
}

record SettlementBatchResponse(
    Long purchaseOrderId,
    String status,
    int affectedCount,
    List<SettlementOrderResponse> items
) {
}

record SettlementPurchaseContext(
    Long purchaseOrderId,
    String purchaseOrderNo,
    Long demandId,
    Long buyerCompanyId,
    String buyerCompanyName,
    String vesselName,
    String materialType,
    String currency,
    String trafficServiceJson,
    BigDecimal fixedFreightFee,
    BigDecimal fixedCustomsFee,
    BigDecimal fixedCraneFee
) {
}

record SupplierSettlementSource(
    Long supplierOrderId,
    Long providerCompanyId,
    String providerName,
    BigDecimal quotedAmount,
    String currency
) {
}

record TrafficSettlementSource(
    Long trafficServiceOrderId,
    Long providerCompanyId,
    String providerName,
    BigDecimal quotedAmount
) {
}

record SettlementDraft(
    String settlementNo,
    String sourceKey,
    Long purchaseOrderId,
    Long supplierOrderId,
    Long trafficServiceOrderId,
    Long buyerCompanyId,
    Long providerCompanyId,
    String providerName,
    String settlementType,
    String materialType,
    BigDecimal quotedAmount,
    BigDecimal actualAmount,
    String currency,
    String status,
    Long createdBy
) {
}
