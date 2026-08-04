package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
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
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenService tokenService;

    @Test
    void registersBasicAccountWithoutCompanyProfile() {
        RegisterRequest request = new RegisterRequest(
            "dock-agent-01",
            null, null, null, null, null, null, null, null, null,
            "secret123", "secret123"
        );
        when(authRepository.userExists("dock-agent-01", null)).thenReturn(false);
        when(passwordHasher.hash("secret123")).thenReturn("hashed-password");
        when(authRepository.insertCompany(
            "",
            "UNSPECIFIED",
            "",
            "",
            null,
            "PROFILE_REQUIRED"
        )).thenReturn(10L);
        when(authRepository.insertUser(10L, "dock-agent-01", null, "hashed-password", "UNSPECIFIED", "PROFILE_REQUIRED"))
            .thenReturn(20L);
        when(tokenService.issue(20L)).thenReturn("dev-token");
        when(authRepository.getUserById(20L)).thenReturn(
            new AuthenticatedUser(20L, "dock-agent-01", null, "hashed-password", "UNSPECIFIED", "PROFILE_REQUIRED", 10L)
        );
        when(authRepository.getCompany(10L)).thenReturn(
            new CompanyResponse(10L, "", "UNSPECIFIED", null, "", "", null, "PROFILE_REQUIRED")
        );
        when(authRepository.rolesForUser(20L)).thenReturn(List.of());

        AuthResponse response = new AuthService(authRepository, passwordHasher, tokenService).register(request);

        assertThat(response.token()).isEqualTo("dev-token");
        assertThat(response.user().username()).isEqualTo("dock-agent-01");
        assertThat(response.user().phone()).isNull();
        assertThat(response.accountStatus()).isEqualTo("PROFILE_REQUIRED");
        assertThat(response.profileStatus()).isEqualTo("PROFILE_REQUIRED");
        assertThat(response.companyStatus()).isEqualTo("PROFILE_REQUIRED");
        assertThat(response.onboardingRequired()).isTrue();
        assertThat(response.requiresOnboarding()).isTrue();
        assertThat(response.defaultRoute()).isEqualTo("/onboarding/company-profile");
        assertThat(response.redirectTo()).isEqualTo("/onboarding/company-profile");
        verify(authRepository).markCompanyOwner(20L);
        verify(authRepository).replaceCompanySupplierServiceTypes(10L, List.of());
    }

    @Test
    void registerOptionsExposeThreeRolesAndSupplierCapabilities() {
        RegisterOptionsResponse options = new AuthService(authRepository, passwordHasher, tokenService).registerOptions();

        assertThat(options.companyTypes()).extracting(OptionResponse::code)
            .containsExactly("SHIP_AGENT", "SUPPLIER", "BARGE_AGENT");
        assertThat(options.supplierServiceTypes()).extracting(OptionResponse::code)
            .containsExactly("MATERIAL", "FOOD");
    }

    @Test
    void rejectsDuplicateAccount() {
        RegisterRequest request = new RegisterRequest(
            "dock-agent-01",
            null, null, null, null, null, null, null, null, null,
            "secret123", "secret123"
        );
        when(authRepository.userExists("dock-agent-01", null)).thenReturn(true);

        assertThatThrownBy(() -> new AuthService(authRepository, passwordHasher, tokenService).register(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409 CONFLICT");

        verify(authRepository, never()).insertCompany(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rejectsPasswordMismatch() {
        RegisterRequest request = new RegisterRequest(
            "dock-agent-01",
            null, null, null, null, null, null, null, null, null,
            "secret123", "different"
        );

        assertThatThrownBy(() -> new AuthService(authRepository, passwordHasher, tokenService).register(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void rejectsMissingPassword() {
        RegisterRequest request = new RegisterRequest(
            "dock-agent-01",
            null, null, null, null, null, null, null, null, null,
            null, null
        );

        assertThatThrownBy(() -> new AuthService(authRepository, passwordHasher, tokenService).register(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void rejectsDisabledUserLoginWithExplicitCode() {
        LoginRequest request = new LoginRequest("ops-01", null, null, "secret123");
        when(authRepository.findUserByAccount("ops-01")).thenReturn(java.util.Optional.of(
            new AuthenticatedUser(30L, "ops-01", null, "hashed-password", "SHIP_AGENT", "DISABLED", 10L)
        ));

        assertThatThrownBy(() -> new AuthService(authRepository, passwordHasher, tokenService).login(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN")
            .hasMessageContaining("USER_DISABLED");

        verify(passwordHasher, never()).matches(anyString(), anyString());
    }

    @Test
    void activeUserMenusComeFromPermissionFilteredRepository() {
        when(tokenService.requireUserId("Bearer token")).thenReturn(30L);
        when(authRepository.getUserById(30L)).thenReturn(
            new AuthenticatedUser(30L, "ops-01", null, "hashed-password", "SHIP_AGENT", "ACTIVE", 10L)
        );
        when(authRepository.getCompany(10L)).thenReturn(
            new CompanyResponse(10L, "Test Company", "SHIP_AGENT", "91330000TEST000001", "Owner", "13800000000", "owner@example.com", "ACTIVE")
        );
        when(authRepository.menusForUser(30L)).thenReturn(List.of(
            new MenuResponse("DASHBOARD", "Dashboard", "/dashboard", "LayoutDashboard", null, 10, "DASHBOARD_VIEW", "SHIP_AGENT", List.of())
        ));
        when(authRepository.buildTree(org.mockito.ArgumentMatchers.anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<MenuResponse> menus = new AuthService(authRepository, passwordHasher, tokenService).menus("Bearer token");

        assertThat(menus).hasSize(1);
        assertThat(menus.get(0).routePath()).isEqualTo("/dashboard");
        verify(authRepository).menusForUser(30L);
    }
}
