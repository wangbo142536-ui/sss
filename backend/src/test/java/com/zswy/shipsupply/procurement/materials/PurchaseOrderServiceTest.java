package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MaterialDemandComparisonService comparisonService;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderService(currentUserService, comparisonService, purchaseOrderRepository);
    }

    @Test
    void createsLowestMixedOrderAndSplitsSupplierOrders() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.findActiveByDemandAndStrategy(22L, 101L, "LOWEST_MIXED"))
            .thenReturn(Optional.empty());
        when(purchaseOrderRepository.nextOrderNo(22L, LocalDate.now())).thenReturn("PO-20260616-001");
        when(purchaseOrderRepository.companyName(22L)).thenReturn("采购商");
        when(comparisonService.comparison("Bearer buyer", 101L)).thenReturn(comparisonWithMixedCandidates());
        when(purchaseOrderRepository.insertOrder(any())).thenReturn(savedDetail(501L, "PO-20260616-001"));

        PurchaseOrderCreateResponse response = service.createFromDemand(
            "Bearer buyer",
            101L,
            new PurchaseOrderCreateRequest(
                null,
                "LOWEST_MIXED",
                "上海港",
                "2026-06-20T09:00:00",
                "2026-06-21T18:00:00",
                "纸箱",
                "尽快配送",
                null
            )
        );

        assertThat(response.purchaseOrderId()).isEqualTo(501L);
        assertThat(response.status()).isEqualTo("PENDING_SUPPLIER_CONFIRM");
        assertThat(response.supplierOrderCount()).isEqualTo(2);
        assertThat(response.itemCount()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("40.50");

        ArgumentCaptor<PurchaseOrderDraft> draftCaptor = ArgumentCaptor.forClass(PurchaseOrderDraft.class);
        verify(purchaseOrderRepository).insertOrder(draftCaptor.capture());
        PurchaseOrderDraft draft = draftCaptor.getValue();
        assertThat(draft.strategyType()).isEqualTo("LOWEST_MIXED");
        assertThat(draft.supplierOrders()).hasSize(2);
        assertThat(draft.supplierOrders()).extracting(PurchaseSupplierOrderDraft::supplierName)
            .containsExactlyInAnyOrder("供应商 A", "供应商 B");
        assertThat(draft.items()).hasSize(2);
        assertThat(draft.items()).extracting(PurchaseOrderItemDraft::quantityFallbackFlag)
            .containsExactly(false, true);
        assertThat(draft.items()).extracting(PurchaseOrderItemDraft::unitMismatchFlag)
            .contains(true);
    }

    @Test
    void createsSingleSupplierOrderFromSingleSupplierStrategy() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.findActiveByDemandAndStrategy(22L, 101L, "SINGLE_SUPPLIER"))
            .thenReturn(Optional.empty());
        when(purchaseOrderRepository.nextOrderNo(22L, LocalDate.now())).thenReturn("PO-20260616-002");
        when(purchaseOrderRepository.companyName(22L)).thenReturn("采购商");
        when(comparisonService.comparison("Bearer buyer", 101L)).thenReturn(comparisonWithMixedCandidates());
        when(purchaseOrderRepository.insertOrder(any())).thenReturn(savedDetail(502L, "PO-20260616-002"));

        service.createFromDemand("Bearer buyer", 101L, new PurchaseOrderCreateRequest(
            null,
            "SINGLE_SUPPLIER",
            "上海港",
            null,
            null,
            "托盘",
            null,
            null
        ));

        ArgumentCaptor<PurchaseOrderDraft> draftCaptor = ArgumentCaptor.forClass(PurchaseOrderDraft.class);
        verify(purchaseOrderRepository).insertOrder(draftCaptor.capture());
        assertThat(draftCaptor.getValue().supplierOrders()).hasSize(1);
        assertThat(draftCaptor.getValue().supplierOrders().get(0).supplierName()).isEqualTo("供应商 A");
        assertThat(draftCaptor.getValue().items()).hasSize(2);
    }

    @Test
    void returnsExistingOrderWhenDemandAndStrategyAlreadyHaveActiveOrder() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.findActiveByDemandAndStrategy(22L, 101L, "LOWEST_MIXED"))
            .thenReturn(Optional.of(summary(501L, "PO-20260616-001")));

        PurchaseOrderCreateResponse response = service.createFromDemand("Bearer buyer", 101L, new PurchaseOrderCreateRequest(
            null,
            "LOWEST_MIXED",
            null,
            null,
            null,
            null,
            null,
            null
        ));

        assertThat(response.purchaseOrderId()).isEqualTo(501L);
        assertThat(response.redirectTo()).isEqualTo("/orders/501");
        verify(comparisonService, never()).comparison("Bearer buyer", 101L);
        verify(purchaseOrderRepository, never()).insertOrder(any());
    }

    @Test
    void rejectsOrderWhenAllRowsHaveNoPricedCandidate() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.findActiveByDemandAndStrategy(22L, 101L, "LOWEST_MIXED"))
            .thenReturn(Optional.empty());
        when(comparisonService.comparison("Bearer buyer", 101L)).thenReturn(comparisonWithoutCandidates());

        assertThatThrownBy(() -> service.createFromDemand("Bearer buyer", 101L, new PurchaseOrderCreateRequest(
            null,
            "LOWEST_MIXED",
            null,
            null,
            null,
            null,
            null,
            null
        )))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("PURCHASE_ORDER_NO_ORDERABLE_ITEMS");
    }

    @Test
    void rejectsOrderWhenSelectedSkuIsNoLongerOnShelfOrPriced() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.findActiveByDemandAndStrategy(22L, 101L, "LOWEST_MIXED"))
            .thenReturn(Optional.empty());
        when(comparisonService.comparison("Bearer buyer", 101L)).thenReturn(comparisonWithInvalidCandidate());

        assertThatThrownBy(() -> service.createFromDemand("Bearer buyer", 101L, new PurchaseOrderCreateRequest(
            null,
            "LOWEST_MIXED",
            null,
            null,
            null,
            null,
            null,
            null
        )))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("PURCHASE_ORDER_SKU_CONFLICT");
    }

    @Test
    void supplierConfirmRequiresExpectedReadyAt() {
        when(currentUserService.requireActiveCompanyUser("Bearer supplier"))
            .thenReturn(new CurrentUserContext(20L, 24L, "ACTIVE", "ACTIVE"));

        assertThatThrownBy(() -> service.confirmSupplierOrder("Bearer supplier", 501L, 601L, new PurchaseSupplierConfirmRequest(
            null,
            null,
            null,
            "纸箱",
            "可以备货"
        )))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("EXPECTED_READY_AT_REQUIRED");
    }

    @Test
    void supplierRejectRequiresReason() {
        when(currentUserService.requireActiveCompanyUser("Bearer supplier"))
            .thenReturn(new CurrentUserContext(20L, 24L, "ACTIVE", "ACTIVE"));

        assertThatThrownBy(() -> service.rejectSupplierOrder("Bearer supplier", 501L, 601L, new PurchaseSupplierRejectRequest(" ")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("REJECT_REASON_REQUIRED");
    }

    private MaterialDemandComparisonResponse comparisonWithMixedCandidates() {
        MaterialDemandSummaryResponse demand = demand();
        MaterialSupplierCandidate supplierAFlat = sku(1L, 24L, "供应商 A", "A-001", "Flat Nose Plier", "611705", "9.00", "PCS", "ON_SHELF");
        MaterialSupplierCandidate supplierBDiagonal = sku(2L, 25L, "供应商 B", "B-001", "Diagonal Cutting Plier", "611706", "13.50", "BOX", "ON_SHELF");
        MaterialSupplierCandidate supplierAFlatSingle = sku(3L, 24L, "供应商 A", "A-003", "Flat Nose Plier", "611705", "10.00", "PCS", "ON_SHELF");
        MaterialSupplierCandidate supplierADiagonalSingle = sku(4L, 24L, "供应商 A", "A-004", "Diagonal Cutting Plier", "611706", "15.00", "PCS", "ON_SHELF");
        return new MaterialDemandComparisonResponse(
            demand,
            new MaterialDemandSupplyInfo("MV BLUE", "待补充", "2026-06-16", "天气待接入", "STATIC_PLACEHOLDER"),
            List.of(),
            List.of(
                item(201L, "611705", "Flat Nose Plier", "3", "PCS", new BigDecimal("3"), null, supplierAFlat, supplierAFlatSingle),
                item(202L, "611706", "Diagonal Cutting Plier", "abc", "PCS", BigDecimal.ONE, "计价数量按 1", supplierBDiagonal, supplierADiagonalSingle)
            )
        );
    }

    private MaterialDemandComparisonResponse comparisonWithoutCandidates() {
        return new MaterialDemandComparisonResponse(
            demand(),
            new MaterialDemandSupplyInfo("MV BLUE", "待补充", "2026-06-16", "天气待接入", "STATIC_PLACEHOLDER"),
            List.of(),
            List.of(item(201L, "611705", "Flat Nose Plier", "3", "PCS", new BigDecimal("3"), null, null, null))
        );
    }

    private MaterialDemandComparisonResponse comparisonWithInvalidCandidate() {
        MaterialSupplierCandidate invalid = sku(1L, 24L, "供应商 A", "A-001", "Flat Nose Plier", "611705", "9.00", "PCS", "OFF_SHELF");
        return new MaterialDemandComparisonResponse(
            demand(),
            new MaterialDemandSupplyInfo("MV BLUE", "待补充", "2026-06-16", "天气待接入", "STATIC_PLACEHOLDER"),
            List.of(),
            List.of(item(201L, "611705", "Flat Nose Plier", "3", "PCS", new BigDecimal("3"), null, invalid, invalid))
        );
    }

    private MaterialDemandSummaryResponse demand() {
        return new MaterialDemandSummaryResponse(
            101L,
            "REQ-20260616-001",
            "APP-001",
            "MV BLUE",
            "2026-06-16",
            "rfq.xlsx",
            "DEMAND_INQUIRY",
            1,
            2,
            2,
            0,
            0,
            "SAVED",
            "2026-06-16T10:00:00",
            "2026-06-16T10:00:00"
        );
    }

    private MaterialDemandComparisonItem item(
        Long itemId,
        String impaCode,
        String productName,
        String quantity,
        String unit,
        BigDecimal pricingQuantity,
        String pricingQuantityNote,
        MaterialSupplierCandidate lowest,
        MaterialSupplierCandidate single
    ) {
        List<MaterialSupplierCandidate> candidates = lowest == null ? List.of() : List.of(lowest);
        return new MaterialDemandComparisonItem(
            itemId,
            Math.toIntExact(itemId - 200),
            impaCode,
            impaCode,
            productName,
            productName,
            "",
            quantity,
            pricingQuantity,
            pricingQuantityNote,
            unit,
            lowest,
            single,
            candidates,
            lowest == null ? "NO_SUPPLIER_CANDIDATE" : null
        );
    }

    private MaterialSupplierCandidate sku(
        Long skuId,
        Long companyId,
        String supplierName,
        String supplierSkuCode,
        String productName,
        String impaCode,
        String unitPrice,
        String stockUnit,
        String shelfStatus
    ) {
        return new MaterialSupplierCandidate(
            skuId,
            companyId,
            supplierName,
            supplierSkuCode,
            productName,
            impaCode,
            impaCode,
            "61",
            "General Tools",
            List.of(),
            productName,
            unitPrice == null ? null : new BigDecimal(unitPrice),
            "CNY",
            "\u00A5",
            new BigDecimal("99"),
            stockUnit,
            "BOX",
            "/files/" + skuId + ".png",
            "/files/" + skuId + "-thumb.png",
            shelfStatus,
            "SPEC_MATCHED",
            "CODE_EXACT",
            "IMPA_OR_PLATFORM_CODE_MATCH"
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
            new BigDecimal("54.00"),
            "CNY",
            "PENDING_SUPPLIER_CONFIRM",
            "尽快配送",
            "2026-06-16T11:00:00",
            "2026-06-16T11:00:00"
        );
    }

    private PurchaseOrderDetailResponse savedDetail(Long orderId, String orderNo) {
        PurchaseOrderSummaryResponse summary = summary(orderId, orderNo);
        return new PurchaseOrderDetailResponse(summary, List.of(), List.of());
    }
}
