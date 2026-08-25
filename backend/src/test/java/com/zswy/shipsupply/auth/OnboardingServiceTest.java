package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private TokenService tokenService;

    @Test
    void submitProfileRejectsMissingUnifiedSocialCreditCode() {
        when(tokenService.requireUserId("Bearer token")).thenReturn(20L);
        when(authRepository.getUserById(20L)).thenReturn(
            new AuthenticatedUser(20L, "supplier-01", null, "hashed", "UNSPECIFIED", "PROFILE_REQUIRED", 10L)
        );
        CompanyProfileRequest request = new CompanyProfileRequest(
            "SUPPLIER",
            "舟山测试供货商",
            " ",
            "张三",
            "13800138000",
            "test@example.com",
            List.of("FILE-1")
        );

        assertThatThrownBy(() -> new OnboardingService(authRepository, tokenService).submitProfile("Bearer token", request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST")
            .hasMessageContaining("UNIFIED_SOCIAL_CREDIT_CODE_REQUIRED")
            .hasMessageContaining("统一社会信用代码必填");

        verify(authRepository, never()).updateCompanyProfile(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void currentProfileDoesNotReturnPlaceholderCompanyNameAsFilledValue() {
        when(tokenService.requireUserId("Bearer token")).thenReturn(20L);
        when(authRepository.getUserById(20L)).thenReturn(
            new AuthenticatedUser(20L, "wangbo1", null, "hashed", "UNSPECIFIED", "PROFILE_REQUIRED", 10L)
        );
        when(authRepository.getCompany(10L)).thenReturn(
            new CompanyResponse(10L, "待完善企业-wangbo1", "UNSPECIFIED", null, "", "", null, "PROFILE_REQUIRED")
        );
        when(authRepository.qualificationsForCompany(10L)).thenReturn(List.of());

        CompanyProfileResponse response = new OnboardingService(authRepository, tokenService).currentProfile("Bearer token");

        assertThat(response.company().companyName()).isEmpty();
        assertThat(response.company().unifiedSocialCreditCode()).isNull();
    }

    @Test
    void supplierProfileRequiresAtLeastOneSupplierCapability() {
        when(tokenService.requireUserId("Bearer token")).thenReturn(20L);
        when(authRepository.getUserById(20L)).thenReturn(
            new AuthenticatedUser(20L, "supplier-01", null, "hashed", "UNSPECIFIED", "PROFILE_REQUIRED", 10L)
        );
        CompanyProfileRequest request = new CompanyProfileRequest(
            "SUPPLIER",
            "舟山测试供货商",
            "91330000TEST000001",
            "张三",
            "13800138000",
            "test@example.com",
            List.of("FILE-1"),
            List.of()
        );

        assertThatThrownBy(() -> new OnboardingService(authRepository, tokenService).submitProfile("Bearer token", request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("SUPPLIER_SERVICE_TYPE_REQUIRED");
    }

    @Test
    void submitProfileRejectsFilePathContactName() {
        when(tokenService.requireUserId("Bearer token")).thenReturn(20L);
        when(authRepository.getUserById(20L)).thenReturn(
            new AuthenticatedUser(20L, "supplier-01", null, "hashed", "UNSPECIFIED", "PROFILE_REQUIRED", 10L)
        );
        CompanyProfileRequest request = new CompanyProfileRequest(
            "SUPPLIER",
            "舟山测试供货商",
            "91330000TEST000001",
            "C:\\fakepath\\license.pdf",
            "13800138000",
            "test@example.com",
            List.of("FILE-1"),
            List.of("MATERIAL")
        );

        assertThatThrownBy(() -> new OnboardingService(authRepository, tokenService).submitProfile("Bearer token", request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("CONTACT_NAME_INVALID");
        verify(authRepository, never()).updateCompanyProfile(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        );
    }
}
