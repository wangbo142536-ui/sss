package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CompanyProfileRepository companyProfileRepository;

    private CompanyProfileService service;

    @BeforeEach
    void setUp() {
        service = new CompanyProfileService(currentUserService, companyProfileRepository);
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(88L, 22L, "ACTIVE", "ACTIVE"));
    }

    @Test
    void savesLogoFieldsAndReadsThemBackFromCompanyProfile() {
        EnterpriseProfileSaveRequest request = new EnterpriseProfileSaveRequest(
            "Zhoushan Supplier",
            "91330000TEST",
            "LOGO-1",
            "/api/files/LOGO-1",
            "Wang Bo",
            "13800000000",
            "wb@example.com"
        );
        EnterpriseProfileResponse saved = profile("LOGO-1", "/api/files/LOGO-1");
        when(companyProfileRepository.saveProfile(22L, request)).thenReturn(saved);
        when(companyProfileRepository.findProfile(22L)).thenReturn(Optional.of(saved));

        EnterpriseProfileResponse saveResponse = service.saveProfile("Bearer token", request);
        EnterpriseProfileResponse getResponse = service.profile("Bearer token");

        assertThat(saveResponse.logoFileId()).isEqualTo("LOGO-1");
        assertThat(saveResponse.logoUrl()).isEqualTo("/api/files/LOGO-1");
        assertThat(getResponse.logoFileId()).isEqualTo("LOGO-1");
        assertThat(getResponse.logoUrl()).isEqualTo("/api/files/LOGO-1");
    }

    @Test
    void defaultsQualificationListToSubmittedStatus() {
        CompanyQualificationResponse qualification = qualification("SUBMITTED");
        when(companyProfileRepository.listQualifications(22L, "SUBMITTED"))
            .thenReturn(List.of(qualification));

        CompanyQualificationListResponse response = service.qualifications("Bearer token", null);

        assertThat(response.items()).containsExactly(qualification);
        verify(companyProfileRepository).listQualifications(22L, "SUBMITTED");
    }

    @Test
    void softDeletesQualificationByMarkingDeletedStatus() {
        when(companyProfileRepository.softDeleteQualification(22L, 9L)).thenReturn(true);

        service.deleteQualification("Bearer token", 9L);

        verify(companyProfileRepository).softDeleteQualification(22L, 9L);
    }

    @Test
    void acceptsQualificationFileUrlAsFileIdAlias() {
        CompanyQualificationResponse qualification = qualification("SUBMITTED");
        when(companyProfileRepository.createQualification(eq(22L), any(CompanyQualificationSaveRequest.class)))
            .thenReturn(qualification);

        service.createQualification("Bearer token", new CompanyQualificationSaveRequest(
            null,
            "business-license.png",
            "/api/files/FILE-1",
            "business_license",
            "Business License",
            null,
            "submitted"
        ));

        verify(companyProfileRepository).createQualification(eq(22L), argThat(request ->
            "FILE-1".equals(request.fileId())
                && "/api/files/FILE-1".equals(request.fileUrl())
                && "BUSINESS_LICENSE".equals(request.qualificationType())
                && "SUBMITTED".equals(request.status())
        ));
    }

    private EnterpriseProfileResponse profile(String logoFileId, String logoUrl) {
        return new EnterpriseProfileResponse(
            22L,
            "Zhoushan Supplier",
            "91330000TEST",
            logoFileId,
            logoUrl,
            "Wang Bo",
            "13800000000",
            "wb@example.com",
            "SUPPLIER",
            "ACTIVE"
        );
    }

    private CompanyQualificationResponse qualification(String status) {
        return new CompanyQualificationResponse(
            9L,
            22L,
            "FILE-1",
            "business-license.png",
            "/api/files/FILE-1",
            "BUSINESS_LICENSE",
            "Business License",
            null,
            status,
            "2026-06-10T09:00:00",
            "2026-06-10T09:05:00"
        );
    }
}
