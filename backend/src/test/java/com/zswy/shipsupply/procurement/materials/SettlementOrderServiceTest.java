package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zswy.shipsupply.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class SettlementOrderServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private SettlementOrderRepository repository;
    @Mock private ServiceEvaluationRepository evaluationRepository;

    private SettlementOrderService service;

    @BeforeEach
    void setUp() {
        service = new SettlementOrderService(
            currentUserService, repository, new ObjectMapper(), evaluationRepository
        );
    }

    @Test
    void automaticallyEnsuresSupplierSettlementAndEvaluationIdempotently() {
        SettlementPurchaseContext purchase = purchase("{\"supplyMode\":\"LAND\"}");
        SupplierSettlementSource supplier = new SupplierSettlementSource(
            601L, 24L, "Supplier A", new BigDecimal("120.00"), "CNY"
        );
        SettlementOrderResponse saved = settlement(701L, "SUPPLIER", 601L, null, 24L);
        when(repository.purchaseContext(22L, 501L)).thenReturn(Optional.of(purchase));
        when(repository.supplierSources(22L, 501L)).thenReturn(List.of(supplier));
        when(repository.upsert(any())).thenReturn(saved);

        service.ensureForPurchaseOrder(22L, 10L, 501L);
        service.ensureForPurchaseOrder(22L, 10L, 501L);

        ArgumentCaptor<SettlementDraft> drafts = ArgumentCaptor.forClass(SettlementDraft.class);
        verify(repository, times(2)).upsert(drafts.capture());
        assertThat(drafts.getAllValues()).allSatisfy(draft -> {
            assertThat(draft.sourceKey()).isEqualTo("SUPPLIER:601");
            assertThat(draft.status()).isEqualTo("PENDING_INVOICE");
        });
        verify(evaluationRepository, times(2)).ensureForSettlement(saved);
        verify(repository, never()).linkExistingTrafficSource(any(), any(), any(), any());
    }

    @Test
    void doesNotCreateBargeSettlementWithoutExistingTrafficOrder() {
        when(repository.purchaseContext(22L, 501L)).thenReturn(Optional.of(purchase("{\"supplyMode\":\"SEA\"}")));
        when(repository.supplierSources(22L, 501L)).thenReturn(List.of());
        when(repository.linkExistingTrafficSource(22L, 501L, 101L, null)).thenReturn(Optional.empty());

        service.ensureForPurchaseOrder(22L, 10L, 501L);

        verify(repository, never()).upsert(any());
        verify(evaluationRepository, never()).ensureForSettlement(any());
    }

    @Test
    void linksExistingTrafficOrderAndCreatesOneBargeSettlement() {
        TrafficSettlementSource traffic = new TrafficSettlementSource(801L, 35L, "Barge Co", new BigDecimal("88.00"));
        SettlementOrderResponse saved = settlement(702L, "BARGE", null, 801L, 35L);
        when(repository.purchaseContext(22L, 501L))
            .thenReturn(Optional.of(purchase("{\"supplyMode\":\"SEA\",\"trafficServiceOrderId\":801}")));
        when(repository.supplierSources(22L, 501L)).thenReturn(List.of());
        when(repository.linkExistingTrafficSource(22L, 501L, 101L, 801L)).thenReturn(Optional.of(traffic));
        when(repository.upsert(any())).thenReturn(saved);

        service.ensureForPurchaseOrder(22L, 10L, 501L);

        ArgumentCaptor<SettlementDraft> draft = ArgumentCaptor.forClass(SettlementDraft.class);
        verify(repository).upsert(draft.capture());
        assertThat(draft.getValue().sourceKey()).isEqualTo("BARGE");
        assertThat(draft.getValue().trafficServiceOrderId()).isEqualTo(801L);
        assertThat(draft.getValue().settlementType()).isEqualTo("BARGE");
        verify(evaluationRepository).ensureForSettlement(saved);
    }

    private SettlementPurchaseContext purchase(String trafficServiceJson) {
        return new SettlementPurchaseContext(
            501L, "PO-501", 101L, 22L, "Buyer Co", "MV BLUE", "MATERIAL", "CNY",
            trafficServiceJson, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    private SettlementOrderResponse settlement(
        Long id, String type, Long supplierOrderId, Long trafficOrderId, Long providerCompanyId
    ) {
        return new SettlementOrderResponse(
            id, "ST-" + id, 501L, "PO-501", "MV BLUE", supplierOrderId, trafficOrderId,
            22L, "Buyer Co", providerCompanyId, "Provider", type, "MATERIAL",
            BigDecimal.ZERO, BigDecimal.ZERO, "CNY", "PENDING_INVOICE", List.of(),
            null, null, 10L, "2026-07-11", "2026-07-11"
        );
    }
}
