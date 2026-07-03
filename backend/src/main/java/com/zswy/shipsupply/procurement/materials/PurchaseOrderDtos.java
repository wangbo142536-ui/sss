package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.util.List;

record PurchaseOrderCreateRequest(
    Long demandId,
    String strategyType,
    String supplyPort,
    String vesselEta,
    String requiredDeliveryTime,
    String deliveryContactName,
    String deliveryContactPhone,
    String deliveryContactEmail,
    String defaultPackagingMethod,
    String buyerRemark,
    List<PurchaseOrderSelectedItemRequest> selectedItems
) {
    PurchaseOrderCreateRequest(
        Long demandId,
        String strategyType,
        String supplyPort,
        String vesselEta,
        String requiredDeliveryTime,
        String defaultPackagingMethod,
        String buyerRemark,
        List<PurchaseOrderSelectedItemRequest> selectedItems
    ) {
        this(demandId, strategyType, supplyPort, vesselEta, requiredDeliveryTime, null, null, null, defaultPackagingMethod, buyerRemark, selectedItems);
    }
}

record PurchaseOrderSelectedItemRequest(
    Long demandItemId,
    Long skuId,
    String quantity,
    BigDecimal pricingQuantity,
    BigDecimal amount,
    String selectedUnit,
    BigDecimal unitPrice,
    BigDecimal unitPriceUsd,
    BigDecimal amountUsd,
    BigDecimal actualQuotePrice,
    BigDecimal actualQuoteAmount,
    BigDecimal quoteMarkupPercent
) {
    PurchaseOrderSelectedItemRequest(Long demandItemId, Long skuId) {
        this(demandItemId, skuId, null, null, null, null, null, null, null, null, null, null);
    }

    PurchaseOrderSelectedItemRequest(Long demandItemId, Long skuId, String quantity, BigDecimal pricingQuantity, BigDecimal amount) {
        this(demandItemId, skuId, quantity, pricingQuantity, amount, null, null, null, null, null, null, null);
    }

    PurchaseOrderSelectedItemRequest(
        Long demandItemId,
        Long skuId,
        String quantity,
        BigDecimal pricingQuantity,
        BigDecimal amount,
        String selectedUnit,
        BigDecimal unitPrice,
        BigDecimal unitPriceUsd,
        BigDecimal amountUsd
    ) {
        this(demandItemId, skuId, quantity, pricingQuantity, amount, selectedUnit, unitPrice, unitPriceUsd, amountUsd, null, null, null);
    }
}

record PurchaseOrderDiscardResult(
    Long demandId,
    int discardedPurchaseOrderCount
) {
}

record PurchaseOrderCreateResponse(
    Long purchaseOrderId,
    String purchaseOrderNo,
    String status,
    int supplierOrderCount,
    int itemCount,
    BigDecimal totalAmount,
    BigDecimal totalAmountUsd,
    String currency,
    String redirectTo,
    boolean idempotent
) {
    PurchaseOrderCreateResponse(
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
        this(purchaseOrderId, purchaseOrderNo, status, supplierOrderCount, itemCount, totalAmount, BigDecimal.ZERO, currency, redirectTo, idempotent);
    }
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
    String inquiryNo,
    String materialType,
    String demandCurrency,
    String recipientCompany,
    String handlerName,
    String handlerEmail,
    Long buyerCompanyId,
    String buyerCompanyName,
    String vesselName,
    String supplyPort,
    String vesselEta,
    String requiredDeliveryTime,
    String deliveryContactName,
    String deliveryContactPhone,
    String deliveryContactEmail,
    String strategyType,
    String strategyName,
    int supplierCount,
    int quotedSupplierCount,
    int totalSupplierCount,
    int itemCount,
    BigDecimal totalAmount,
    BigDecimal totalAmountUsd,
    String currency,
    String status,
    String packagingMethod,
    Long supplierOrderId,
    String expectedReadyAt,
    String buyerRemark,
    String createdAt,
    String updatedAt
) {
    PurchaseOrderSummaryResponse(
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
        int quotedSupplierCount,
        int totalSupplierCount,
        int itemCount,
        BigDecimal totalAmount,
        String currency,
        String status,
        String buyerRemark,
        String createdAt,
        String updatedAt
    ) {
        this(purchaseOrderId, purchaseOrderNo, demandId, demandNo, applicationNo, null, null, null, null, null, null,
            buyerCompanyId, buyerCompanyName, vesselName, supplyPort, vesselEta, requiredDeliveryTime, null, null, null, strategyType,
            strategyName, supplierCount, quotedSupplierCount, totalSupplierCount, itemCount, totalAmount, BigDecimal.ZERO, currency,
            status, null, null, null, buyerRemark, createdAt, updatedAt);
    }

    PurchaseOrderSummaryResponse(
        Long purchaseOrderId,
        String purchaseOrderNo,
        Long demandId,
        String demandNo,
        String applicationNo,
        String inquiryNo,
        String materialType,
        String demandCurrency,
        String recipientCompany,
        String handlerName,
        String handlerEmail,
        Long buyerCompanyId,
        String buyerCompanyName,
        String vesselName,
        String supplyPort,
        String vesselEta,
        String requiredDeliveryTime,
        String strategyType,
        String strategyName,
        int supplierCount,
        int quotedSupplierCount,
        int totalSupplierCount,
        int itemCount,
        BigDecimal totalAmount,
        String currency,
        String status,
        String buyerRemark,
        String createdAt,
        String updatedAt
    ) {
        this(purchaseOrderId, purchaseOrderNo, demandId, demandNo, applicationNo, inquiryNo, materialType, demandCurrency,
            recipientCompany, handlerName, handlerEmail, buyerCompanyId, buyerCompanyName, vesselName,
            supplyPort, vesselEta, requiredDeliveryTime, null, null, null, strategyType, strategyName, supplierCount, quotedSupplierCount,
            totalSupplierCount, itemCount, totalAmount, BigDecimal.ZERO, currency, status, null, null, null, buyerRemark, createdAt, updatedAt);
    }
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
    String readyAt,
    String suppliedAt,
    String deliveryImageFileId,
    String deliveryImageUrl,
    String deliveryRemark,
    String rejectedAt,
    List<PurchaseOrderItemResponse> items
) {
    PurchaseSupplierOrderResponse(
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
        this(supplierOrderId, supplierOrderNo, purchaseOrderId, supplierCompanyId, supplierName, status, itemCount,
            subtotalAmount, discountType, discountValue, discountAmount, finalAmount, currency, packagingMethod,
            expectedReadyAt, supplierRemark, rejectReason, confirmedAt, null, null, null, null, null, rejectedAt, items);
    }
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
    BigDecimal unitPriceUsd,
    BigDecimal amount,
    BigDecimal amountUsd,
    BigDecimal actualQuotePrice,
    String actualQuoteCurrency,
    BigDecimal quoteMarkupPercent,
    BigDecimal quoteProfitAmount,
    String currency,
    boolean unitMismatchFlag,
    boolean quantityFallbackFlag,
    String sourceMatchType,
    String sourceReason
) {
    PurchaseOrderItemResponse(
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
        this(itemId, supplierOrderId, purchaseOrderId, demandItemId, skuId, supplierSkuCode, platformCode, impaCode,
            productName, specification, quantity, unit, pricingQuantity, unitPrice, null, amount, null, null, null, null, null, currency,
            unitMismatchFlag, quantityFallbackFlag, sourceMatchType, sourceReason);
    }
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

record PurchaseSupplierSupplyCompleteRequest(
    String deliveryImageFileId,
    String deliveryImageUrl,
    String deliveryRemark
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
    String deliveryContactName,
    String deliveryContactPhone,
    String deliveryContactEmail,
    String strategyType,
    String strategyName,
    int supplierCount,
    int itemCount,
    BigDecimal totalAmount,
    BigDecimal totalAmountUsd,
    String currency,
    String status,
    String buyerRemark,
    Long createdBy,
    List<PurchaseSupplierOrderDraft> supplierOrders,
    List<PurchaseOrderItemDraft> items
) {
    PurchaseOrderDraft(
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
        this(orderNo, demandId, demandNo, applicationNo, buyerCompanyId, buyerCompanyName, vesselName, supplyPort, vesselEta,
            requiredDeliveryTime, null, null, null, strategyType, strategyName, supplierCount, itemCount, totalAmount, BigDecimal.ZERO, currency,
            status, buyerRemark, createdBy, supplierOrders, items);
    }
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
    BigDecimal unitPriceUsd,
    BigDecimal amount,
    BigDecimal amountUsd,
    BigDecimal actualQuotePrice,
    String actualQuoteCurrency,
    BigDecimal quoteMarkupPercent,
    BigDecimal quoteProfitAmount,
    String currency,
    boolean unitMismatchFlag,
    boolean quantityFallbackFlag,
    String sourceMatchType,
    String sourceReason
) {
    PurchaseOrderItemDraft(
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
        this(supplierOrderNo, demandItemId, skuId, supplierSkuCode, platformCode, impaCode, productName, specification,
            quantity, unit, pricingQuantity, unitPrice, null, amount, null, null, null, null, null, currency, unitMismatchFlag, quantityFallbackFlag,
            sourceMatchType, sourceReason);
    }
}
