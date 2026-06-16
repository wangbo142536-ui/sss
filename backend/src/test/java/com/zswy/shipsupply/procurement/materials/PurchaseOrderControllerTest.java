package com.zswy.shipsupply.procurement.materials;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PurchaseOrderController.class)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseOrderService purchaseOrderService;

    @Test
    void createsPurchaseOrderFromDemandComparison() throws Exception {
        PurchaseOrderCreateRequest request = createRequest(101L, "LOWEST_MIXED");
        when(purchaseOrderService.createFromDemand("Bearer buyer", 101L, request))
            .thenReturn(createResponse(501L, "PO-20260616-001"));

        mockMvc.perform(post("/api/procurement/material-demands/101/purchase-orders")
                .header("Authorization", "Bearer buyer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.purchaseOrderId").value(501))
            .andExpect(jsonPath("$.purchaseOrderNo").value("PO-20260616-001"))
            .andExpect(jsonPath("$.status").value("PENDING_SUPPLIER_CONFIRM"))
            .andExpect(jsonPath("$.redirectTo").value("/orders/501"));
    }

    @Test
    void supportsCompatibilityPathFromComparison() throws Exception {
        PurchaseOrderCreateRequest request = createRequest(101L, "SINGLE_SUPPLIER");
        when(purchaseOrderService.createFromDemand("Bearer buyer", null, request))
            .thenReturn(createResponse(502L, "PO-20260616-002"));

        mockMvc.perform(post("/api/procurement/orders/from-comparison")
                .header("Authorization", "Bearer buyer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.purchaseOrderId").value(502));
    }

    @Test
    void listsBuyerPurchaseOrders() throws Exception {
        when(purchaseOrderService.listBuyer(
            "Bearer buyer",
            "APP-001",
            "PENDING_SUPPLIER_CONFIRM",
            "供应商 A",
            "2026-06-01",
            "2026-06-30",
            "2026-06-20",
            "2026-06-21",
            1,
            20
        )).thenReturn(new PurchaseOrderListResponse(List.of(summary(501L, "PO-20260616-001")), 1, 20, 1L));

        mockMvc.perform(get("/api/procurement/purchase-orders")
                .header("Authorization", "Bearer buyer")
                .param("keyword", "APP-001")
                .param("status", "PENDING_SUPPLIER_CONFIRM")
                .param("supplier", "供应商 A")
                .param("createdFrom", "2026-06-01")
                .param("createdTo", "2026-06-30")
                .param("deliveryFrom", "2026-06-20")
                .param("deliveryTo", "2026-06-21")
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].purchaseOrderId").value(501))
            .andExpect(jsonPath("$.items[0].purchaseOrderNo").value("PO-20260616-001"))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void returnsBuyerPurchaseOrderDetail() throws Exception {
        when(purchaseOrderService.buyerDetail("Bearer buyer", 501L)).thenReturn(detail(501L, "PO-20260616-001"));

        mockMvc.perform(get("/api/procurement/purchase-orders/501")
                .header("Authorization", "Bearer buyer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.order.purchaseOrderId").value(501))
            .andExpect(jsonPath("$.supplierOrders[0].supplierCompanyId").value(24))
            .andExpect(jsonPath("$.supplierOrders[0].items[0].skuId").value(1));
    }

    @Test
    void listsSupplierOrders() throws Exception {
        when(purchaseOrderService.listSupplier("Bearer supplier", "PO-20260616", "PENDING_SUPPLIER_CONFIRM", null, null, 1, 20))
            .thenReturn(new PurchaseOrderListResponse(List.of(summary(501L, "PO-20260616-001")), 1, 20, 1L));

        mockMvc.perform(get("/api/supplier/procurement/orders")
                .header("Authorization", "Bearer supplier")
                .param("keyword", "PO-20260616")
                .param("status", "PENDING_SUPPLIER_CONFIRM")
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].purchaseOrderId").value(501));
    }

    @Test
    void confirmsSupplierOrder() throws Exception {
        PurchaseSupplierConfirmRequest request = new PurchaseSupplierConfirmRequest(
            "2026-06-18T10:00:00",
            "AMOUNT",
            new BigDecimal("5.00"),
            "托盘",
            "可以备货"
        );
        when(purchaseOrderService.confirmSupplierOrder("Bearer supplier", 501L, 601L, request))
            .thenReturn(detail(501L, "PO-20260616-001"));

        mockMvc.perform(post("/api/procurement/purchase-orders/501/supplier-orders/601/confirm")
                .header("Authorization", "Bearer supplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.order.purchaseOrderId").value(501));
    }

    @Test
    void rejectsSupplierOrder() throws Exception {
        PurchaseSupplierRejectRequest request = new PurchaseSupplierRejectRequest("库存不足");
        when(purchaseOrderService.rejectSupplierOrder("Bearer supplier", 501L, 601L, request))
            .thenReturn(detail(501L, "PO-20260616-001"));

        mockMvc.perform(post("/api/procurement/purchase-orders/501/supplier-orders/601/reject")
                .header("Authorization", "Bearer supplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.order.purchaseOrderId").value(501));
    }

    private PurchaseOrderCreateRequest createRequest(Long demandId, String strategyType) {
        return new PurchaseOrderCreateRequest(
            demandId,
            strategyType,
            "上海港",
            "2026-06-20T09:00:00",
            "2026-06-21T18:00:00",
            "纸箱",
            "尽快配送",
            null
        );
    }

    private PurchaseOrderCreateResponse createResponse(Long orderId, String orderNo) {
        return new PurchaseOrderCreateResponse(
            orderId,
            orderNo,
            "PENDING_SUPPLIER_CONFIRM",
            2,
            2,
            new BigDecimal("40.50"),
            "CNY",
            "/orders/" + orderId,
            false
        );
    }

    private PurchaseOrderSummaryResponse summary(Long orderId, String orderNo) {
        return new PurchaseOrderSummaryResponse(
            orderId,
            orderNo,
            101L,
            "REQ-20260616-001",
            "APP-001",
            22L,
            "采购商",
            "MV BLUE",
            "上海港",
            "2026-06-20T09:00:00",
            "2026-06-21T18:00:00",
            "LOWEST_MIXED",
            "最低混供",
            2,
            2,
            new BigDecimal("40.50"),
            "CNY",
            "PENDING_SUPPLIER_CONFIRM",
            "尽快配送",
            "2026-06-16T11:00:00",
            "2026-06-16T11:00:00"
        );
    }

    private PurchaseOrderDetailResponse detail(Long orderId, String orderNo) {
        PurchaseOrderItemResponse item = new PurchaseOrderItemResponse(
            701L,
            601L,
            orderId,
            201L,
            1L,
            "A-001",
            "611705",
            "611705",
            "Flat Nose Plier",
            "160MM",
            "3",
            "PCS",
            new BigDecimal("3"),
            new BigDecimal("9.00"),
            new BigDecimal("27.00"),
            "CNY",
            false,
            false,
            "CODE_EXACT",
            "IMPA_OR_PLATFORM_CODE_MATCH"
        );
        PurchaseSupplierOrderResponse supplierOrder = new PurchaseSupplierOrderResponse(
            601L,
            orderNo + "-S01",
            orderId,
            24L,
            "供应商 A",
            "PENDING_SUPPLIER_CONFIRM",
            1,
            new BigDecimal("27.00"),
            null,
            null,
            BigDecimal.ZERO,
            new BigDecimal("27.00"),
            "CNY",
            "纸箱",
            null,
            null,
            null,
            null,
            null,
            List.of(item)
        );
        return new PurchaseOrderDetailResponse(summary(orderId, orderNo), List.of(supplierOrder), List.of());
    }
}
