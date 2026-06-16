package com.zswy.shipsupply.procurement.materials;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping("/api/procurement/material-demands/{demandId}/purchase-orders")
    public PurchaseOrderCreateResponse createFromDemand(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long demandId,
        @RequestBody PurchaseOrderCreateRequest request
    ) {
        return purchaseOrderService.createFromDemand(authorizationHeader, demandId, request);
    }

    @PostMapping("/api/procurement/orders/from-comparison")
    public PurchaseOrderCreateResponse createFromComparison(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody PurchaseOrderCreateRequest request
    ) {
        return purchaseOrderService.createFromDemand(authorizationHeader, null, request);
    }

    @GetMapping("/api/procurement/purchase-orders")
    public PurchaseOrderListResponse listBuyerOrders(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "supplier", required = false) String supplier,
        @RequestParam(value = "createdFrom", required = false) String createdFrom,
        @RequestParam(value = "createdTo", required = false) String createdTo,
        @RequestParam(value = "deliveryFrom", required = false) String deliveryFrom,
        @RequestParam(value = "deliveryTo", required = false) String deliveryTo,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return purchaseOrderService.listBuyer(
            authorizationHeader,
            keyword,
            status,
            supplier,
            createdFrom,
            createdTo,
            deliveryFrom,
            deliveryTo,
            page,
            size
        );
    }

    @GetMapping("/api/procurement/purchase-orders/{orderId}")
    public PurchaseOrderDetailResponse buyerDetail(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId
    ) {
        return purchaseOrderService.buyerDetail(authorizationHeader, orderId);
    }

    @GetMapping("/api/supplier/procurement/orders")
    public PurchaseOrderListResponse listSupplierOrders(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "createdFrom", required = false) String createdFrom,
        @RequestParam(value = "createdTo", required = false) String createdTo,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return purchaseOrderService.listSupplier(authorizationHeader, keyword, status, createdFrom, createdTo, page, size);
    }

    @PostMapping("/api/procurement/purchase-orders/{orderId}/supplier-orders/{supplierOrderId}/confirm")
    public PurchaseOrderDetailResponse confirmSupplierOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId,
        @PathVariable Long supplierOrderId,
        @RequestBody PurchaseSupplierConfirmRequest request
    ) {
        return purchaseOrderService.confirmSupplierOrder(authorizationHeader, orderId, supplierOrderId, request);
    }

    @PostMapping("/api/procurement/purchase-orders/{orderId}/supplier-orders/{supplierOrderId}/reject")
    public PurchaseOrderDetailResponse rejectSupplierOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long orderId,
        @PathVariable Long supplierOrderId,
        @RequestBody PurchaseSupplierRejectRequest request
    ) {
        return purchaseOrderService.rejectSupplierOrder(authorizationHeader, orderId, supplierOrderId, request);
    }
}
