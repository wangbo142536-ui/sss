package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class MaterialDemandComparisonServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MaterialDemandRepository materialDemandRepository;

    @Mock
    private MaterialSupplierCandidateProvider supplierCandidateProvider;

    private MaterialDemandComparisonService service;

    @BeforeEach
    void setUp() {
        service = new MaterialDemandComparisonService(
            currentUserService,
            materialDemandRepository,
            supplierCandidateProvider
        );
    }

    @Test
    void rejectsMissingOrCrossCompanyDemand() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.findSummaryById(22L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comparison("Bearer token", 999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("MATERIAL_DEMAND_NOT_FOUND");
    }

    @Test
    void returnsEmptyComparisonWhenThereAreNoSupplierCandidates() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.findSummaryById(22L, 101L)).thenReturn(Optional.of(summary()));
        when(materialDemandRepository.items(22L, 101L)).thenReturn(List.of(item(201L, "611705", "Flat Nose Plier", "160MM", "3")));
        when(supplierCandidateProvider.findOnShelfCandidates()).thenReturn(List.of());

        MaterialDemandComparisonResponse response = service.comparison("Bearer token", 101L);

        assertThat(response.demand().demandId()).isEqualTo(101L);
        assertThat(response.supplyInfo().supplyVessel()).isEqualTo("MV BLUE");
        assertThat(response.supplyInfo().supplyPort()).isEqualTo("待补充");
        assertThat(response.supplyInfo().weatherSource()).isEqualTo("STATIC_PLACEHOLDER");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).candidates()).isEmpty();
        assertThat(response.items().get(0).emptyReason()).isEqualTo("NO_SUPPLIER_CANDIDATE");
        assertThat(response.strategies()).extracting(MaterialDemandComparisonStrategy::strategyType)
            .containsExactly("LOWEST_MIXED", "SINGLE_SUPPLIER");
        assertThat(response.strategies().get(0).matchedCount()).isZero();
        assertThat(response.strategies().get(0).unmatchedCount()).isEqualTo(1);
    }

    @Test
    void calculatesLowestMixedAndSingleSupplierStrategiesFromOnShelfCandidates() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.findSummaryById(22L, 101L)).thenReturn(Optional.of(summary()));
        when(materialDemandRepository.items(22L, 101L)).thenReturn(List.of(
            item(201L, "611705", "Flat Nose Plier", "160MM", "3"),
            item(202L, "611706", "Diagonal Cutting Plier", "180MM", "2")
        ));
        when(supplierCandidateProvider.findOnShelfCandidates()).thenReturn(List.of(
            sku(1L, 24L, "供应商 A", "A-PLIERS-1", "Flat Nose Plier", "611705", "9.00", "99", "ON_SHELF"),
            sku(2L, 24L, "供应商 A", "A-PLIERS-2", "Diagonal Cutting Plier", "611706", "15.00", "99", "ON_SHELF"),
            sku(3L, 25L, "供应商 B", "B-PLIERS-1", "Flat Nose Plier", "611705", "8.00", "99", "ON_SHELF"),
            sku(4L, 25L, "供应商 B", "B-PLIERS-2", "Diagonal Cutting Plier", "611706", "20.00", "99", "ON_SHELF"),
            sku(5L, 26L, "供应商 C", "C-HIDDEN", "Diagonal Cutting Plier", "611706", "1.00", "99", "OFF_SHELF")
        ));

        MaterialDemandComparisonResponse response = service.comparison("Bearer token", 101L);

        assertThat(response.items().get(0).lowestCandidate().supplierName()).isEqualTo("供应商 B");
        assertThat(response.items().get(1).lowestCandidate().supplierName()).isEqualTo("供应商 A");
        assertThat(response.items().get(1).candidates()).extracting(MaterialSupplierCandidate::supplierName)
            .doesNotContain("供应商 C");

        MaterialDemandComparisonStrategy lowestMixed = response.strategies().get(0);
        assertThat(lowestMixed.strategyType()).isEqualTo("LOWEST_MIXED");
        assertThat(lowestMixed.matchedCount()).isEqualTo(2);
        assertThat(lowestMixed.totalAmount()).isEqualByComparingTo("54.00");
        assertThat(lowestMixed.suppliers()).hasSize(2);

        MaterialDemandComparisonStrategy singleSupplier = response.strategies().get(1);
        assertThat(singleSupplier.strategyType()).isEqualTo("SINGLE_SUPPLIER");
        assertThat(singleSupplier.matchedCount()).isEqualTo(2);
        assertThat(singleSupplier.totalAmount()).isEqualByComparingTo("57.00");
        assertThat(singleSupplier.suppliers()).hasSize(1);
        assertThat(singleSupplier.suppliers().get(0).supplierName()).isEqualTo("供应商 A");
        assertThat(response.items()).allSatisfy(item -> assertThat(item.singleSupplierCandidate().supplierName()).isEqualTo("供应商 A"));
    }

    @Test
    void invalidQuantityUsesOneForPricingAndReturnsNote() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.findSummaryById(22L, 101L)).thenReturn(Optional.of(summary()));
        when(materialDemandRepository.items(22L, 101L)).thenReturn(List.of(item(201L, "611705", "Flat Nose Plier", "160MM", "abc")));
        when(supplierCandidateProvider.findOnShelfCandidates()).thenReturn(List.of(
            sku(1L, 24L, "供应商 A", "A-PLIERS-1", "Flat Nose Plier", "611705", "9.00", "99", "ON_SHELF")
        ));

        MaterialDemandComparisonResponse response = service.comparison("Bearer token", 101L);

        assertThat(response.items().get(0).pricingQuantity()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(response.items().get(0).pricingQuantityNote()).isEqualTo("计价数量按 1");
        assertThat(response.strategies().get(0).totalAmount()).isEqualByComparingTo("9.00");
    }

    private MaterialDemandSummaryResponse summary() {
        return new MaterialDemandSummaryResponse(
            101L,
            "REQ-20260616-001",
            "APP-001",
            "MV BLUE",
            "2026-06-16",
            "STCL26SG001-R01.xlsx",
            "DEMAND_INQUIRY",
            27,
            2,
            1,
            1,
            0,
            "SAVED",
            "2026-06-16T10:00:00",
            "2026-06-16T10:05:00"
        );
    }

    private MaterialDemandItemResponse item(Long itemId, String impaCode, String description, String sizeModel, String quantity) {
        return new MaterialDemandItemResponse(
            itemId,
            "DEMAND_INQUIRY",
            27,
            Math.toIntExact(itemId - 200),
            27 + Math.toIntExact(itemId - 200),
            27 + Math.toIntExact(itemId - 200),
            Map.of("IMPA", impaCode, "DESCRIPTION", description),
            impaCode,
            description,
            sizeModel,
            quantity,
            "PCS",
            "",
            "",
            description,
            null,
            "",
            "",
            impaCode,
            impaCode,
            description,
            description,
            sizeModel,
            "EXACT",
            "Exact Match",
            "CODE_MATCH",
            false,
            null,
            null,
            List.of(),
            List.of()
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
        String stockQty,
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
            List.of(new MaterialSupplierSkuAttribute("size", "Size", productName, null, 0, productName)),
            productName,
            new BigDecimal(unitPrice),
            "CNY",
            "\u00A5",
            new BigDecimal(stockQty),
            "PCS",
            "BOX",
            "/files/" + skuId + ".png",
            "/files/" + skuId + "-thumb.png",
            shelfStatus,
            "SPEC_MATCHED",
            null,
            null
        );
    }
}
