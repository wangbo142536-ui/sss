package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RegistrationSubmissionServiceTest {

    @Mock AuthRepository authRepository;
    @Mock AuthService authService;
    @Mock PasswordHasher passwordHasher;
    @Mock TokenService tokenService;
    @Mock FileStorageService fileStorageService;

    @Test
    void createsPendingReviewAccountCompanyAndQualificationTogether() {
        RegistrationSubmissionRequest request = request("SUPPLIER", List.of("MATERIAL", "FOOD"), "secret123");
        MockMultipartFile file = new MockMultipartFile("files", "license.pdf", "application/pdf", "pdf".getBytes());
        AuthResponse expected = org.mockito.Mockito.mock(AuthResponse.class);

        when(authService.normalizeSelfServiceRole("SUPPLIER")).thenReturn("SUPPLIER");
        when(authService.normalizeSupplierServiceTypes("SUPPLIER", List.of("MATERIAL", "FOOD"), true))
            .thenReturn(List.of("MATERIAL", "FOOD"));
        when(authRepository.userExists("supplier-atomic", "13800138000")).thenReturn(false);
        when(authRepository.insertCompany("舟山原子供货商", "SUPPLIER", "张三", "13800138000", "test@example.com", "PENDING_REVIEW"))
            .thenReturn(10L);
        when(passwordHasher.hash("secret123")).thenReturn("hashed");
        when(authRepository.insertUser(10L, "supplier-atomic", "13800138000", "hashed", "SUPPLIER", "PENDING_REVIEW"))
            .thenReturn(20L);
        when(fileStorageService.storeForUser(20L, file))
            .thenReturn(new FileUploadResponse("FILE-1", "license.pdf", "application/pdf", 3L, "/api/files/FILE-1"));
        when(tokenService.issue(20L)).thenReturn("token");
        when(authService.buildAuthResponse(20L, "token")).thenReturn(expected);

        AuthResponse actual = service().registerAndSubmit(request, List.of(file));

        assertThat(actual).isSameAs(expected);
        verify(authRepository).updateCompanyProfile(
            10L, "SUPPLIER", "舟山原子供货商", "91330000TEST000001", "张三", "13800138000",
            "test@example.com", "PENDING_REVIEW", null
        );
        verify(authRepository).replaceCompanySupplierServiceTypes(10L, List.of("MATERIAL", "FOOD"));
        verify(authRepository).replaceCompanyQualifications(10L, List.of("FILE-1"));
        verify(authRepository).markCompanyOwner(20L);
    }

    @Test
    void rejectsInvalidRequestBeforeCreatingAccount() {
        RegistrationSubmissionRequest request = request("SHIP_AGENT", List.of(), "different");
        MockMultipartFile file = new MockMultipartFile("files", "license.pdf", "application/pdf", "pdf".getBytes());

        assertThatThrownBy(() -> service().registerAndSubmit(request, List.of(file)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("confirmPassword does not match password");
        verify(authRepository, never()).insertCompany(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()
        );
        verify(fileStorageService, never()).storeForUser(anyLong(), org.mockito.ArgumentMatchers.any());
    }

    private RegistrationSubmissionService service() {
        return new RegistrationSubmissionService(authRepository, authService, passwordHasher, tokenService, fileStorageService);
    }

    private RegistrationSubmissionRequest request(String companyType, List<String> services, String confirmation) {
        return new RegistrationSubmissionRequest(
            "supplier-atomic", "secret123", confirmation, companyType, services, "舟山原子供货商",
            "91330000TEST000001", "张三", "13800138000", "test@example.com"
        );
    }
}
