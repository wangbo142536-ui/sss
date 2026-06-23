package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
class MaterialDemandServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MaterialDemandRepository materialDemandRepository;

    private MaterialDemandService service;

    @BeforeEach
    void setUp() {
        service = new MaterialDemandService(currentUserService, materialDemandRepository);
    }

    @Test
    void createsDemandWithCompanyScopedDailyNumberAndStatistics() {
        when(currentUserService.requireActiveCompanyUser("Bearer company-token"))
            .thenReturn(new CurrentUserContext(10L, 1L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.nextDemandNo(1L, LocalDate.of(2026, 6, 5)))
            .thenReturn("REQ-20260605-001");
        when(materialDemandRepository.insertDemand(eq(1L), eq(10L), eq("REQ-20260605-001"), any(), any()))
            .thenReturn(101L);

        MaterialDemandSaveResponse response = service.save("Bearer company-token", saveRequest(null, null), null);

        assertThat(response.demandId()).isEqualTo(101L);
        assertThat(response.demandNo()).isEqualTo("REQ-20260605-001");
        assertThat(response.redirectTo()).isEqualTo("/procurement/materials?demandId=101");

        ArgumentCaptor<MaterialDemandStats> statsCaptor = ArgumentCaptor.forClass(MaterialDemandStats.class);
        verify(materialDemandRepository).insertDemand(eq(1L), eq(10L), eq("REQ-20260605-001"), any(), statsCaptor.capture());
        assertThat(statsCaptor.getValue().skuCount()).isEqualTo(3);
        assertThat(statsCaptor.getValue().exactCount()).isEqualTo(1);
        assertThat(statsCaptor.getValue().similarCount()).isEqualTo(1);
        assertThat(statsCaptor.getValue().unmatchedCount()).isEqualTo(1);
        verify(materialDemandRepository).replaceItems(eq(101L), eq(1L), any());
    }

    @Test
    void updatesDemandWhenDemandIdBelongsToCurrentCompany() {
        when(currentUserService.requireActiveCompanyUser("Bearer company-token"))
            .thenReturn(new CurrentUserContext(10L, 1L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.findSummaryById(1L, 101L)).thenReturn(Optional.of(summary(101L)));

        MaterialDemandSaveResponse response = service.save("Bearer company-token", saveRequest(null, null), 101L);

        assertThat(response.demandId()).isEqualTo(101L);
        assertThat(response.demandNo()).isEqualTo("REQ-20260605-001");
        verify(materialDemandRepository).updateDemand(eq(101L), eq(1L), eq(10L), any(), any());
        verify(materialDemandRepository).replaceItems(eq(101L), eq(1L), any());
        verify(materialDemandRepository, never()).insertDemand(eq(1L), eq(10L), any(), any(), any());
    }

    @Test
    void updatesComparingDemandWithoutLockingQuantityAndUnitEdits() {
        when(currentUserService.requireActiveCompanyUser("Bearer company-token"))
            .thenReturn(new CurrentUserContext(10L, 1L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.findSummaryById(1L, 101L)).thenReturn(Optional.of(summary(101L, "COMPARING")));

        MaterialDemandSaveResponse response = service.save("Bearer company-token", saveRequest(null, null), 101L);

        assertThat(response.demandId()).isEqualTo(101L);
        assertThat(response.status()).isEqualTo("COMPARING");
        verify(materialDemandRepository).updateDemand(eq(101L), eq(1L), eq(10L), any(), any());
        verify(materialDemandRepository).replaceItems(eq(101L), eq(1L), any());
    }

    @Test
    void rejectsSaveWhenItemQuantityIsNotPositive() {
        when(currentUserService.requireActiveCompanyUser("Bearer company-token"))
            .thenReturn(new CurrentUserContext(10L, 1L, "ACTIVE", "ACTIVE"));

        assertThatThrownBy(() -> service.save("Bearer company-token", saveRequestWithQuantity("0"), null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("MATERIAL_DEMAND_ITEM_QUANTITY_INVALID");

        verify(materialDemandRepository, never()).insertDemand(eq(1L), eq(10L), any(), any(), any());
    }

    @Test
    void createsDemandWhenApplicationNoIsBlank() {
        when(currentUserService.requireActiveCompanyUser("Bearer company-token"))
            .thenReturn(new CurrentUserContext(10L, 1L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.nextDemandNo(1L, LocalDate.of(2026, 6, 5)))
            .thenReturn("REQ-20260605-001");
        when(materialDemandRepository.insertDemand(eq(1L), eq(10L), eq("REQ-20260605-001"), any(), any()))
            .thenReturn(101L);

        MaterialDemandSaveResponse response = service.save("Bearer company-token", saveRequest(null, null, " "), null);

        assertThat(response.demandId()).isEqualTo(101L);
        ArgumentCaptor<MaterialDemandSaveRequest> requestCaptor = ArgumentCaptor.forClass(MaterialDemandSaveRequest.class);
        verify(materialDemandRepository).insertDemand(eq(1L), eq(10L), eq("REQ-20260605-001"), requestCaptor.capture(), any());
        assertThat(requestCaptor.getValue().applicationNo()).isNull();
    }

    @Test
    void rejectsCrossCompanyDemandUpdate() {
        when(currentUserService.requireActiveCompanyUser("Bearer company-token"))
            .thenReturn(new CurrentUserContext(10L, 1L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.findSummaryById(1L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save("Bearer company-token", saveRequest(null, null), 999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404 NOT_FOUND")
            .hasMessageContaining("MATERIAL_DEMAND_NOT_FOUND");

        verify(materialDemandRepository, never()).replaceItems(eq(999L), eq(1L), any());
    }

    @Test
    void updatesDemandByDemandNoWhenRequestContainsDemandNo() {
        when(currentUserService.requireActiveCompanyUser("Bearer company-token"))
            .thenReturn(new CurrentUserContext(10L, 1L, "ACTIVE", "ACTIVE"));
        when(materialDemandRepository.findSummaryByNo(1L, "REQ-20260605-001")).thenReturn(Optional.of(summary(101L)));

        MaterialDemandSaveResponse response = service.save("Bearer company-token", saveRequest(null, "REQ-20260605-001"), null);

        assertThat(response.demandId()).isEqualTo(101L);
        verify(materialDemandRepository).updateDemand(eq(101L), eq(1L), eq(10L), any(), any());
    }

    @Test
    void listsAndLoadsOnlyCurrentCompanyDemandData() {
        when(currentUserService.requireActiveCompanyUser("Bearer company-token"))
            .thenReturn(new CurrentUserContext(10L, 1L, "ACTIVE", "ACTIVE"));
        MaterialDemandListResponse listResponse = new MaterialDemandListResponse(List.of(summary(101L)), 1, 20, 1L);
        when(materialDemandRepository.list(1L, "APP-001", "SAVED", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 1, 20))
            .thenReturn(listResponse);
        when(materialDemandRepository.findSummaryById(1L, 101L)).thenReturn(Optional.of(summary(101L)));
        when(materialDemandRepository.items(1L, 101L)).thenReturn(List.of(itemResponse(201L)));

        assertThat(service.list("Bearer company-token", "APP-001", "SAVED", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 1, 20).total())
            .isEqualTo(1L);
        assertThat(service.detail("Bearer company-token", 101L).items()).hasSize(1);
    }

    @Test
    void generatesNextDemandNumberForSameCompanyAndDay() {
        assertThat(MaterialDemandNo.next(LocalDate.of(2026, 6, 5), null)).isEqualTo("REQ-20260605-001");
        assertThat(MaterialDemandNo.next(LocalDate.of(2026, 6, 5), "REQ-20260605-001")).isEqualTo("REQ-20260605-002");
        assertThat(MaterialDemandNo.next(LocalDate.of(2026, 6, 5), "REQ-20260605-099")).isEqualTo("REQ-20260605-100");
    }

    private MaterialDemandSaveRequest saveRequest(Long demandId, String demandNo) {
        return saveRequest(demandId, demandNo, "APP-001");
    }

    private MaterialDemandSaveRequest saveRequest(Long demandId, String demandNo, String applicationNo) {
        return new MaterialDemandSaveRequest(
            demandId,
            demandNo,
            applicationNo,
            "MV BLUE",
            "ZHOUSHAN",
            "舟山港",
            "2026-06-05T09:30",
            "2026-06-05",
            "STCL26SD003-R01.xlsx",
            "DEMAND_INQUIRY",
            27,
            List.of(
                item(1, "EXACT"),
                item(2, "SIMILAR"),
                item(3, "UNMATCHED")
            )
        );
    }

    private MaterialDemandSaveRequest saveRequestWithQuantity(String quantity) {
        return new MaterialDemandSaveRequest(
            null,
            null,
            "APP-001",
            "MV BLUE",
            null,
            null,
            null,
            "2026-06-05",
            "STCL26SD003-R01.xlsx",
            "DEMAND_INQUIRY",
            27,
            List.of(itemWithQuantity(1, "EXACT", quantity, "PCS"))
        );
    }

    private MaterialDemandItemRequest item(int sequence, String matchResult) {
        return itemWithQuantity(sequence, matchResult, "2", "PCS");
    }

    private MaterialDemandItemRequest itemWithQuantity(int sequence, String matchResult, String quantity, String unit) {
        return new MaterialDemandItemRequest(
            "DEMAND_INQUIRY",
            27,
            sequence,
            27 + sequence,
            27 + sequence,
            Map.of("IMPA", "11010" + sequence, "DESCRIPTION", "COTTON RAG"),
            "11010" + sequence,
            "COTTON RAG",
            "WHITE",
            quantity,
            unit,
            "urgent",
            null,
            null,
            null,
            null,
            null,
            "11010" + sequence,
            "11010" + sequence,
            "棉布",
            "COTTON RAG",
            "WHITE",
            matchResult,
            matchResult,
            "CODE_MATCH",
            false,
            null,
            null,
            List.of(new MaterialMatchCandidate(
                "11010" + sequence,
                "11",
                "Deck",
                "棉布",
                "COTTON RAG",
                "WHITE",
                "PCS",
                "CODE_MATCH",
                "CODE_MATCH"
            )),
            List.of()
        );
    }

    private MaterialDemandSummaryResponse summary(Long demandId) {
        return summary(demandId, "SAVED");
    }

    private MaterialDemandSummaryResponse summary(Long demandId, String status) {
        return new MaterialDemandSummaryResponse(
            demandId,
            "REQ-20260605-001",
            "APP-001",
            "MV BLUE",
            "ZHOUSHAN",
            "舟山港",
            "2026-06-05T09:30",
            "2026-06-05",
            "STCL26SD003-R01.xlsx",
            "DEMAND_INQUIRY",
            27,
            3,
            1,
            1,
            1,
            status,
            "2026-06-05T10:00:00",
            "2026-06-05T10:05:00"
        );
    }

    private MaterialDemandItemResponse itemResponse(Long itemId) {
        MaterialDemandItemRequest request = item(1, "EXACT");
        return new MaterialDemandItemResponse(
            itemId,
            request.documentType(),
            request.headerRowIndex(),
            request.sequence(),
            request.sourceRowNo(),
            request.sourceRowNumber(),
            request.rawColumns(),
            request.impaCode(),
            request.description(),
            request.sizeModel(),
            request.quantity(),
            request.unit(),
            request.remarks(),
            request.supplierItemNo(),
            request.rawNameSpec(),
            request.price(),
            request.packing(),
            request.stock(),
            request.selectedImpaCode(),
            request.candidateImpaCode(),
            request.candidateNameCn(),
            request.candidateNameEn(),
            request.candidateSpec(),
            request.matchResult(),
            request.matchResultName(),
            request.reason(),
            request.hasImage(),
            request.imageIndex(),
            request.imageAnchor(),
            request.candidateSnapshot(),
            request.candidates()
        );
    }
}
