package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;
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
class PlatformAccountServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private TokenService tokenService;

    private PlatformAccountService service;

    @BeforeEach
    void setUp() {
        service = new PlatformAccountService(authRepository, tokenService);
        when(tokenService.requireUserId("Bearer admin-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", null, "hash", "PLATFORM_ADMIN", "ACTIVE", 1L)
        );
        when(authRepository.rolesForUser(1L)).thenReturn(List.of(
            new RoleResponse("PLATFORM_ADMIN", "平台管理员", "PLATFORM_ADMIN")
        ));
    }

    @Test
    void filtersCompaniesAndAccountsWithoutExposingOtherRows() {
        PlatformAccountResponse owner = account(10L, "owner", "REGISTERED_ADMIN", "ACTIVE");
        PlatformAccountResponse child = account(11L, "buyer", "INTERNAL_CREATED", "DISABLED");
        when(authRepository.platformCompanyAccounts()).thenReturn(List.of(
            new PlatformCompanyAccountsResponse(
                2L,
                "舟山双业务供应商",
                "SUPPLIER",
                List.of("MATERIAL", "FOOD"),
                "ACTIVE",
                2,
                1,
                List.of(owner, child)
            )
        ));

        PlatformCompanyAccountPageResponse response = service.accounts(
            "Bearer admin-token",
            "SUPPLIER",
            "FOOD",
            "INTERNAL_CREATED",
            null,
            "DISABLED",
            null,
            1,
            20
        );

        assertThat(response.totalCompanies()).isEqualTo(1);
        assertThat(response.totalAccounts()).isEqualTo(1);
        assertThat(response.items().get(0).accounts()).extracting(PlatformAccountResponse::userId)
            .containsExactly(11L);
    }

    @Test
    void disablingAccountRevokesTokensAndWritesReasonedAudit() {
        PlatformAccountResponse active = account(10L, "owner", "REGISTERED_ADMIN", "ACTIVE");
        PlatformAccountResponse disabled = account(10L, "owner", "REGISTERED_ADMIN", "DISABLED");
        when(authRepository.findPlatformAccount(10L)).thenReturn(Optional.of(active), Optional.of(disabled));

        PlatformAccountResponse response = service.updateStatus(
            "Bearer admin-token",
            10L,
            new PlatformAccountStatusRequest("DISABLED", "企业申请暂停账号")
        );

        assertThat(response.status()).isEqualTo("DISABLED");
        verify(authRepository).updateMemberStatus(10L, "DISABLED");
        verify(tokenService).invalidateUser(10L);
        verify(authRepository).log(
            1L,
            "UPDATE_PLATFORM_ACCOUNT_STATUS",
            "USER",
            "10",
            "/api/platform/accounts/10/status",
            "status=ACTIVE->DISABLED;reason=企业申请暂停账号"
        );
    }

    private PlatformAccountResponse account(Long id, String username, String source, String status) {
        return new PlatformAccountResponse(
            id,
            username,
            username,
            null,
            null,
            "SUPPLIER",
            source,
            "REGISTERED_ADMIN".equals(source),
            List.of("COMPANY_ADMIN_2"),
            status,
            "2026-08-04T10:00:00",
            null
        );
    }
}
