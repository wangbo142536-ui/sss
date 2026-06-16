package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.util.List;

record PurchaseOrderCreateRequest(
    Long demandId,
    String strategyType,
    String supplyPort,
    String vesselEta,
    String requiredDeliveryTime,
    String defaultPackagingMethod,
    String buyerRemark,
    List<PurchaseOrderSelectedItemRequest> selectedItems
) {
}

record PurchaseOrderSelectedItemRequest(
    Long demandItemId,
    Long skuId
) {
}

record PurchaseOrderCreateResponse(
    Long purchaseOrderId,
    String purchaseOrderNo,
    String status,
    int supplierOrderCount,
    int itemCount,
    BigDecimal totalAmount,
    String currency,
    String redirectTo,
    boolean idempotent
) {
}

record PurchaseOrderListResponse(
    List<PurchaseOrderSummaryResponse> items,
    int page,
    int size,
    long total
) {
}

record PurchaseOrderSummaryResponse(
    Long purchaseOrderId,
    String purchaseOrderNo,
    Long demandId,
    String demandNo,
    String applicationNo,
    Long buyerCompanyId,
    String buyerCompanyName,
    String vesselName,
    String supplyPort,
    String vesselEta,
    String requiredDeliveryTime,
    String strategyType,
    String strategyName,
    int supplierCount,
    int itemCount,
    BigDecimal totalAmount,
    String currency,
    String status,
    String buyerRemark,
    String createdAt,
    String updatedAt
) {
}

record PurchaseOrderDetailResponse(
    PurchaseOrderSummaryResponse order,
    List<PurchaseSupplierOrderResponse> supplierOrders,
    List<PurchaseOrderEventResponse> events
) {
}

record PurchaseSupplierOrderResponse(
    Long supplierOrderId,
    String supplierOrderNo,
    Long purchaseOrderId,
    Long supplierCompanyId,
    String supplierName,
    String status,
    int itemCount,
    BigDecimal subtotalAmount,
    String discountType,
    BigDecimal discountValue,
    BigDecimal discountAmount,
    BigDecimal finalAmount,
    String currency,
    String packagingMethod,
    String expectedReadyAt,
    String supplierRemark,
    String rejectReason,
    String confirmedAt,
    String rejectedAt,
    List<PurchaseOrderItemResponse> items
) {
}

record PurchaseOrderItemResponse(
    Long itemId,
    Long supplierOrderId,
    Long purchaseOrderId,
    Long demandItemId,
    Long skuId,
    String supplierSkuCode,
    String platformCode,
    String impaCode,
    String productName,
    String specification,
    String quantity,
    String unit,
    BigDecimal pricingQuantity,
    BigDecimal unitPrice,
    BigDecimal amount,
    String currency,
    boolean unitMismatchFlag,
    boolean quantityFallbackFlag,
    String sourceMatchType,
    String sourceReason
) {
}

record PurchaseOrderEventResponse(
    Long eventId,
    Long purchaseOrderId,
    Long supplierOrderId,
    String eventType,
    String eventMessage,
    Long operatorUserId,
    Long operatorCompanyId,
    String createdAt
) {
}

record PurchaseSupplierConfirmRequest(
    String expectedReadyAt,
    String discountType,
    BigDecimal discountValue,
    String packagingMethod,
    String supplierRemark
) {
}

record PurchaseSupplierRejectRequest(
    String rejectReason
) {
}

record PurchaseOrderDraft(
    String orderNo,
    Long demandId,
    String demandNo,
    String applicationNo,
    Long buyerCompanyId,
    String buyerCompanyName,
    String vesselName,
    String supplyPort,
    String vesselEta,
    String requiredDeliveryTime,
    String strategyType,
    String strategyName,
    int supplierCount,
    int itemCount,
    BigDecimal totalAmount,
    String currency,
    String status,
    String buyerRemark,
    Long createdBy,
    List<PurchaseSupplierOrderDraft> supplierOrders,
    List<PurchaseOrderItemDraft> items
) {
}

record PurchaseSupplierOrderDraft(
    String supplierOrderNo,
    Long supplierCompanyId,
    String supplierName,
    String status,
    int itemCount,
    BigDecimal subtotalAmount,
    BigDecimal finalAmount,
    String currency,
    String packagingMethod
) {
}

record PurchaseOrderItemDraft(
    String supplierOrderNo,
    Long demandItemId,
    Long skuId,
    String supplierSkuCode,
    String platformCode,
    String impaCode,
    String productName,
    String specification,
    String quantity,
    String unit,
    BigDecimal pricingQuantity,
    BigDecimal unitPrice,
    BigDecimal amount,
    String currency,
    boolean unitMismatchFlag,
    boolean quantityFallbackFlag,
    String sourceMatchType,
    String sourceReason
) {
}
