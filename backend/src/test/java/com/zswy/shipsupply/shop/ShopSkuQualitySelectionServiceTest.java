package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.shop.quality.api.QualitySelectionSaveRequest;
import com.zswy.shipsupply.shop.quality.application.ShopSkuQualitySelectionService;
import com.zswy.shipsupply.shop.quality.domain.QualitySelectionAuditEntry;
import com.zswy.shipsupply.shop.quality.domain.ShopSkuQualitySelection;
import com.zswy.shipsupply.shop.quality.infrastructure.ShopSkuQualitySelectionRepository;

@ExtendWith(MockitoExtension.class)
class ShopSkuQualitySelectionServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock AuthRepository authRepository;
    @Mock ShopSkuQualitySelectionRepository repository;

    private ShopSkuQualitySelectionService service;

    @BeforeEach
    void setUp() {
        service = new ShopSkuQualitySelectionService(currentUserService, authRepository, repository);
        when(currentUserService.requireActiveCompanyUser("Bearer admin"))
            .thenReturn(new CurrentUserContext(3L, 3L, "ACTIVE", "ACTIVE"));
    }

    @Test
    void onlyPlatformAdministratorCanSaveQualitySelection() {
        when(authRepository.hasRole(3L, "PLATFORM_ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> service.save("Bearer admin", 35L, 471L, request()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("PLATFORM_ADMIN_REQUIRED");
    }

    @Test
    void savesIndependentQcRecordWithAuditAndReportUrl() {
        when(authRepository.hasRole(3L, "PLATFORM_ADMIN")).thenReturn(true);
        when(repository.isSupplierSku(35L, 471L)).thenReturn(true);
        when(repository.activeFileExists("QC-FILE-1")).thenReturn(true);
        when(repository.save(eq(35L), eq(471L), eq(3L), any(LocalDateTime.class), any(), any(), eq("QC-FILE-1"), eq("qc-report.pdf"), any()))
            .thenReturn(saved());

        var response = service.save("Bearer admin", 35L, 471L, request());

        assertThat(response.inspectionConclusion()).isEqualTo("符合海事严选要求");
        assertThat(response.reportUrl()).isEqualTo("/api/files/QC-FILE-1");
        assertThat(response.auditTrail()).hasSize(1);
        verify(authRepository).log(eq(3L), eq("SHOP_SKU_QUALITY_SELECTION_SAVE"), eq("SHOP_SKU"), eq("471"), any(), any());
    }

    private QualitySelectionSaveRequest request() {
        return new QualitySelectionSaveRequest(
            "2026-08-21T10:30",
            "外观、规格与资料核验",
            "平台QC人员完成抽检并留痕",
            "QC-FILE-1",
            "qc-report.pdf",
            "符合海事严选要求"
        );
    }

    private ShopSkuQualitySelection saved() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 21, 10, 30);
        return new ShopSkuQualitySelection(
            1L,
            35L,
            471L,
            now,
            "外观、规格与资料核验",
            "平台QC人员完成抽检并留痕",
            "QC-FILE-1",
            "qc-report.pdf",
            "符合海事严选要求",
            List.of(new QualitySelectionAuditEntry(now.toString(), 3L, "CREATED", "外观、规格与资料核验", "平台QC人员完成抽检并留痕", "符合海事严选要求", "QC-FILE-1", "qc-report.pdf")),
            "ACTIVE",
            3L,
            3L,
            now,
            now
        );
    }
}
