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

    @Mock
    private SettlementOrderService settlementOrderService;

    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderService(
            currentUserService, comparisonService, purchaseOrderRepository, null, settlementOrderService
        );
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
        assertThat(draft.items()).extracting(PurchaseOrderItemDraft::quoteMarkupPercent)
            .allMatch(value -> value != null && value.compareTo(new BigDecimal("10")) == 0);
        assertThat(draft.items()).extracting(PurchaseOrderItemDraft::actualQuotePrice)
            .containsExactly(new BigDecimal("9.90000000"), new BigDecimal("14.85000000"));
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
    void createsOrderOnlyForSelectedComparisonRows() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.findActiveByDemandAndStrategy(22L, 101L, "LOWEST_MIXED"))
            .thenReturn(Optional.empty());
        when(purchaseOrderRepository.nextOrderNo(22L, LocalDate.now())).thenReturn("PO-20260616-003");
        when(purchaseOrderRepository.companyName(22L)).thenReturn("采购商");
        when(comparisonService.comparison("Bearer buyer", 101L)).thenReturn(comparisonWithMixedCandidates());
        when(purchaseOrderRepository.insertOrder(any())).thenReturn(savedDetail(503L, "PO-20260616-003"));

        service.createFromDemand("Bearer buyer", 101L, new PurchaseOrderCreateRequest(
            null,
            "LOWEST_MIXED",
            null,
            null,
            null,
            "UNIFIED_PACKAGING",
            null,
            List.of(new PurchaseOrderSelectedItemRequest(202L, 2L))
        ));

        ArgumentCaptor<PurchaseOrderDraft> draftCaptor = ArgumentCaptor.forClass(PurchaseOrderDraft.class);
        verify(purchaseOrderRepository).insertOrder(draftCaptor.capture());
        PurchaseOrderDraft draft = draftCaptor.getValue();
        assertThat(draft.supplierOrders()).hasSize(1);
        assertThat(draft.supplierOrders().get(0).supplierName()).isEqualTo("供应商 B");
        assertThat(draft.items()).hasSize(1);
        assertThat(draft.items().get(0).demandItemId()).isEqualTo(202L);
        assertThat(draft.items().get(0).skuId()).isEqualTo(2L);
    }

    @Test
    void createsOrderUsingSelectedItemQuantityWhenDemandQuantityIsMissing() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.findActiveByDemandAndStrategy(22L, 101L, "LOWEST_MIXED"))
            .thenReturn(Optional.empty());
        when(purchaseOrderRepository.nextOrderNo(22L, LocalDate.now())).thenReturn("PO-20260616-004");
        when(purchaseOrderRepository.companyName(22L)).thenReturn("采购商");
        when(comparisonService.comparison("Bearer buyer", 101L)).thenReturn(comparisonWithMixedCandidates());
        when(purchaseOrderRepository.insertOrder(any())).thenReturn(savedDetail(504L, "PO-20260616-004"));

        service.createFromDemand("Bearer buyer", 101L, new PurchaseOrderCreateRequest(
            null,
            "LOWEST_MIXED",
            null,
            null,
            null,
            "UNIFIED_PACKAGING",
            null,
            List.of(new PurchaseOrderSelectedItemRequest(202L, 2L, "5", new BigDecimal("5"), new BigDecimal("67.50")))
        ));

        ArgumentCaptor<PurchaseOrderDraft> draftCaptor = ArgumentCaptor.forClass(PurchaseOrderDraft.class);
        verify(purchaseOrderRepository).insertOrder(draftCaptor.capture());
        PurchaseOrderDraft draft = draftCaptor.getValue();
        assertThat(draft.totalAmount()).isEqualByComparingTo("67.50");
        assertThat(draft.items()).hasSize(1);
        assertThat(draft.items().get(0).quantity()).isEqualTo("5");
        assertThat(draft.items().get(0).pricingQuantity()).isEqualByComparingTo("5");
        assertThat(draft.items().get(0).amount()).isEqualByComparingTo("67.50");
        assertThat(draft.items().get(0).quantityFallbackFlag()).isFalse();
    }

    @Test
    void createsOrderUsingSelectedUnitPriceSnapshot() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.findActiveByDemandAndStrategy(22L, 101L, "LOWEST_MIXED"))
            .thenReturn(Optional.empty());
        when(purchaseOrderRepository.nextOrderNo(22L, LocalDate.now())).thenReturn("PO-20260616-005");
        when(purchaseOrderRepository.companyName(22L)).thenReturn("采购商");
        when(comparisonService.comparison("Bearer buyer", 101L)).thenReturn(comparisonWithMixedCandidates());
        when(purchaseOrderRepository.insertOrder(any())).thenReturn(savedDetail(505L, "PO-20260616-005"));

        service.createFromDemand("Bearer buyer", 101L, new PurchaseOrderCreateRequest(
            null,
            "LOWEST_MIXED",
            null,
            null,
            null,
            "UNIFIED_PACKAGING",
            null,
            List.of(new PurchaseOrderSelectedItemRequest(
                202L,
                2L,
                "2",
                new BigDecimal("2"),
                new BigDecimal("300.00"),
                "BOX",
                new BigDecimal("150.00"),
                new BigDecimal("21.4286"),
                new BigDecimal("42.8572")
            ))
        ));

        ArgumentCaptor<PurchaseOrderDraft> draftCaptor = ArgumentCaptor.forClass(PurchaseOrderDraft.class);
        verify(purchaseOrderRepository).insertOrder(draftCaptor.capture());
        PurchaseOrderDraft draft = draftCaptor.getValue();
        assertThat(draft.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(draft.totalAmountUsd()).isEqualByComparingTo("42.8572");
        assertThat(draft.items().get(0).unit()).isEqualTo("BOX");
        assertThat(draft.items().get(0).unitPrice()).isEqualByComparingTo("150.00");
        assertThat(draft.items().get(0).unitPriceUsd()).isEqualByComparingTo("21.4286");
        assertThat(draft.items().get(0).amount()).isEqualByComparingTo("300.00");
        assertThat(draft.items().get(0).amountUsd()).isEqualByComparingTo("42.8572");
    }

    @Test
    void discardsDemandAndLinkedPurchaseOrdersFromDemand() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.discardByDemand(22L, 101L, 10L)).thenReturn(2);

        MaterialDemandStatusResponse response = service.discardByDemand("Bearer buyer", 101L);

        assertThat(response.demandId()).isEqualTo(101L);
        assertThat(response.status()).isEqualTo("DISCARDED");
        assertThat(response.discardedPurchaseOrderCount()).isEqualTo(2);
        verify(purchaseOrderRepository).discardByDemand(22L, 101L, 10L);
    }

    @Test
    void repeatedDiscardReturnsCurrentDiscardedStateWithoutNewOrderChanges() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.discardByDemand(22L, 101L, 10L)).thenReturn(0);

        MaterialDemandStatusResponse response = service.discardByDemand("Bearer buyer", 101L);

        assertThat(response.demandId()).isEqualTo(101L);
        assertThat(response.status()).isEqualTo("DISCARDED");
        assertThat(response.discardedPurchaseOrderCount()).isZero();
        verify(purchaseOrderRepository).discardByDemand(22L, 101L, 10L);
    }

    @Test
    void discardsDemandAndLinkedPurchaseOrdersFromPurchaseOrder() {
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.discardByOrder(22L, 501L, 10L)).thenReturn(new PurchaseOrderDiscardResult(101L, 2));

        MaterialDemandStatusResponse response = service.discardByPurchaseOrder("Bearer buyer", 501L);

        assertThat(response.demandId()).isEqualTo(101L);
        assertThat(response.status()).isEqualTo("DISCARDED");
        assertThat(response.discardedPurchaseOrderCount()).isEqualTo(2);
        verify(purchaseOrderRepository).discardByOrder(22L, 501L, 10L);
    }

    @Test
    void rejectsSupplierActionWhenOrderIsDiscarded() {
        when(currentUserService.requireActiveCompanyUser("Bearer supplier"))
            .thenReturn(new CurrentUserContext(20L, 24L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.isOrderDiscarded(501L)).thenReturn(true);

        assertThatThrownBy(() -> service.confirmSupplierOrder("Bearer supplier", 501L, 601L, new PurchaseSupplierConfirmRequest(
            "2026-06-18T10:00:00",
            null,
            null,
            "UNIFIED_PACKAGING",
            null
        )))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("PURCHASE_ORDER_DISCARDED");
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
        verify(settlementOrderService).ensureForPurchaseOrder(22L, 10L, 501L);
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
    void supplierMarksReadyAndSuppliedWithDeliveryImage() {
        when(currentUserService.requireActiveCompanyUser("Bearer supplier"))
            .thenReturn(new CurrentUserContext(30L, 24L, "ACTIVE", "ACTIVE"));
        when(purchaseOrderRepository.isOrderDiscarded(501L)).thenReturn(false);
        when(purchaseOrderRepository.markSupplierReady(24L, 30L, 501L, 601L)).thenReturn(Optional.of(savedDetail(501L, "PO-20260616-001")));
        when(purchaseOrderRepository.markSupplierSupplied(24L, 30L, 501L, 601L, new PurchaseSupplierSupplyCompleteRequest(
            "FILE-1",
            "/api/files/FILE-1",
            "已拍照供船"
        ))).thenReturn(Optional.of(savedDetail(501L, "PO-20260616-001")));

        PurchaseOrderDetailResponse ready = service.markSupplierReady("Bearer supplier", 501L, 601L);
        PurchaseOrderDetailResponse supplied = service.markSupplierSupplied("Bearer supplier", 501L, 601L, new PurchaseSupplierSupplyCompleteRequest(
            "FILE-1",
            "/api/files/FILE-1",
            "已拍照供船"
        ));

        assertThat(ready.order().purchaseOrderId()).isEqualTo(501L);
        assertThat(supplied.order().purchaseOrderId()).isEqualTo(501L);
        verify(purchaseOrderRepository).markSupplierReady(24L, 30L, 501L, 601L);
        verify(purchaseOrderRepository).markSupplierSupplied(24L, 30L, 501L, 601L, new PurchaseSupplierSupplyCompleteRequest(
            "FILE-1",
            "/api/files/FILE-1",
            "已拍照供船"
        ));
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
            new MaterialDemandSupplyInfo("MV BLUE", "舟山港", "ZHOUSHAN", "舟山港", "2026-06-16T12:30", "2026-06-16", "天气待接入", "STATIC_PLACEHOLDER"),
            List.of(),
            List.of(
                item(201L, "611705", "Flat Nose Plier", "3", "PCS", new BigDecimal("3"), null, supplierAFlat, supplierAFlatSingle),
                item(202L, "611706", "Diagonal Cutting Plier", "abc", "PCS", BigDecimal.ONE, "计价数量按 1", supplierBDiagonal, supplierADiagonalSingle)
            ),
            false,
            false,
            null
        );
    }

    private MaterialDemandComparisonResponse comparisonWithoutCandidates() {
        return new MaterialDemandComparisonResponse(
            demand(),
            new MaterialDemandSupplyInfo("MV BLUE", "舟山港", "ZHOUSHAN", "舟山港", "2026-06-16T12:30", "2026-06-16", "天气待接入", "STATIC_PLACEHOLDER"),
            List.of(),
            List.of(item(201L, "611705", "Flat Nose Plier", "3", "PCS", new BigDecimal("3"), null, null, null)),
            false,
            false,
            null
        );
    }

    private MaterialDemandComparisonResponse comparisonWithInvalidCandidate() {
        MaterialSupplierCandidate invalid = sku(1L, 24L, "供应商 A", "A-001", "Flat Nose Plier", "611705", "9.00", "PCS", "OFF_SHELF");
        return new MaterialDemandComparisonResponse(
            demand(),
            new MaterialDemandSupplyInfo("MV BLUE", "舟山港", "ZHOUSHAN", "舟山港", "2026-06-16T12:30", "2026-06-16", "天气待接入", "STATIC_PLACEHOLDER"),
            List.of(),
            List.of(item(201L, "611705", "Flat Nose Plier", "3", "PCS", new BigDecimal("3"), null, invalid, invalid)),
            false,
            false,
            null
        );
    }

    private MaterialDemandSummaryResponse demand() {
        return new MaterialDemandSummaryResponse(
            101L,
            "REQ-20260616-001",
            "APP-001",
            "MV BLUE",
            "ZHOUSHAN",
            "舟山港",
            "2026-06-16T12:30",
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
            impaCode,
            productName,
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
