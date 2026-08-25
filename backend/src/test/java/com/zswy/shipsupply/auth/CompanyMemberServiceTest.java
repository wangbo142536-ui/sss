package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CompanyMemberServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordHasher passwordHasher;

    private CompanyMemberService service;

    @BeforeEach
    void setUp() {
        service = new CompanyMemberService(authRepository, tokenService, passwordHasher);
    }

    @Test
    void rejectsMemberCreationBeforeCompanyIsActive() {
        when(tokenService.requireUserId("Bearer owner-token")).thenReturn(10L);
        when(authRepository.getUserById(10L)).thenReturn(user(10L, 1L, "ACTIVE"));
        when(authRepository.getCompany(1L)).thenReturn(company(1L, "PENDING_REVIEW"));
        when(authRepository.isCompanyOwner(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.createMember("Bearer owner-token", createRequest("ops-01")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN")
            .hasMessageContaining("COMPANY_NOT_ACTIVE");

        verify(authRepository, never()).insertCompanyMember(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void rejectsMemberManagementByNonOwner() {
        when(tokenService.requireUserId("Bearer child-token")).thenReturn(20L);
        when(authRepository.getUserById(20L)).thenReturn(user(20L, 1L, "ACTIVE"));
        when(authRepository.getCompany(1L)).thenReturn(company(1L, "ACTIVE"));
        when(authRepository.isCompanyOwner(20L)).thenReturn(false);

        when(authRepository.rolesForUser(20L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.members("Bearer child-token", null, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN")
            .hasMessageContaining("NOT_COMPANY_ADMIN");
    }

    @Test
    void rejectsPlatformAdministratorFromCompanyMemberManagement() {
        when(tokenService.requireUserId("Bearer platform-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", null, "hash", "PLATFORM_ADMIN", "ACTIVE", 99L)
        );
        when(authRepository.getCompany(99L)).thenReturn(company(99L, "ACTIVE"));

        assertThatThrownBy(() -> service.members("Bearer platform-token", null, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN")
            .hasMessageContaining("PLATFORM_ADMIN_MUST_USE_PLATFORM_ACCOUNT_MANAGEMENT");

        verify(authRepository, never()).companyMembers(anyLong(), anyString(), anyString());
    }

    @Test
    void activeOwnerCreatesMemberAndAssignsRoles() {
        when(tokenService.requireUserId("Bearer owner-token")).thenReturn(10L);
        when(authRepository.getUserById(10L)).thenReturn(user(10L, 1L, "ACTIVE"));
        when(authRepository.getCompany(1L)).thenReturn(company(1L, "ACTIVE"));
        when(authRepository.isCompanyOwner(10L)).thenReturn(true);
        when(authRepository.userExists("ops-01", "13800000001")).thenReturn(false);
        when(passwordHasher.hash("Temp@123456")).thenReturn("hashed");
        when(authRepository.companyAssignableRoles(1L)).thenReturn(assignableRoles());
        when(authRepository.insertCompanyMember(1L, "ops-01", null, "13800000001", null, "hashed", "SHIP_AGENT", 10L)).thenReturn(30L);
        when(authRepository.findCompanyMember(1L, 30L)).thenReturn(Optional.of(member(30L, "ops-01", "ACTIVE", false)));
        when(authRepository.roleCodesForUser(30L)).thenReturn(List.of("SHIP_AGENT"));
        when(authRepository.userMenuPermissionKeys(30L)).thenReturn(List.of());

        CompanyMemberResponse response = service.createMember("Bearer owner-token", createRequest("ops-01"));

        assertThat(response.userId()).isEqualTo(30L);
        assertThat(response.username()).isEqualTo("ops-01");
        verify(authRepository).replaceCompanyUserRoles(30L, 1L, List.of("SHIP_AGENT"));
    }

    @Test
    void crossCompanyMemberIsNotFound() {
        when(tokenService.requireUserId("Bearer owner-token")).thenReturn(10L);
        when(authRepository.getUserById(10L)).thenReturn(user(10L, 1L, "ACTIVE"));
        when(authRepository.getCompany(1L)).thenReturn(company(1L, "ACTIVE"));
        when(authRepository.isCompanyOwner(10L)).thenReturn(true);
        when(authRepository.findCompanyMember(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMemberStatus("Bearer owner-token", 99L, new CompanyMemberStatusRequest("DISABLED")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404 NOT_FOUND")
            .hasMessageContaining("MEMBER_NOT_FOUND");
    }

    @Test
    void resetPasswordUpdatesHashForCompanyMember() {
        when(tokenService.requireUserId("Bearer owner-token")).thenReturn(10L);
        when(authRepository.getUserById(10L)).thenReturn(user(10L, 1L, "ACTIVE"));
        when(authRepository.getCompany(1L)).thenReturn(company(1L, "ACTIVE"));
        when(authRepository.isCompanyOwner(10L)).thenReturn(true);
        when(authRepository.findCompanyMember(1L, 30L)).thenReturn(Optional.of(member(30L, "ops-01", "ACTIVE", false)));
        when(passwordHasher.hash("New@123456")).thenReturn("new-hash");

        service.resetMemberPassword("Bearer owner-token", 30L, new CompanyMemberResetPasswordRequest("New@123456"));

        verify(authRepository).updatePassword(30L, "new-hash");
    }

    @Test
    void assignsRolesForCompanyMember() {
        when(tokenService.requireUserId("Bearer owner-token")).thenReturn(10L);
        when(authRepository.getUserById(10L)).thenReturn(user(10L, 1L, "ACTIVE"));
        when(authRepository.getCompany(1L)).thenReturn(company(1L, "ACTIVE"));
        when(authRepository.isCompanyOwner(10L)).thenReturn(true);
        when(authRepository.findCompanyMember(1L, 30L)).thenReturn(Optional.of(member(30L, "ops-01", "ACTIVE", false)));
        when(authRepository.companyAssignableRoles(1L)).thenReturn(assignableRoles());

        service.updateMemberRoles("Bearer owner-token", 30L, new CompanyMemberRolesRequest(List.of("SUPPLIER")));

        verify(authRepository).replaceCompanyUserRoles(30L, 1L, List.of("SUPPLIER"));
    }

    private CompanyMemberCreateRequest createRequest(String username) {
        return new CompanyMemberCreateRequest(username, null, "13800000001", null, "Temp@123456", List.of("SHIP_AGENT"), null, null);
    }

    private AuthenticatedUser user(Long id, Long companyId, String status) {
        return new AuthenticatedUser(id, "owner", "13800000000", "hash", "SHIP_AGENT", status, companyId);
    }

    private CompanyResponse company(Long id, String status) {
        return new CompanyResponse(id, "Test Company", "SHIP_AGENT", "91330000TEST000001", "Owner", "13800000000", "owner@example.com", status);
    }

    private CompanyMemberResponse member(Long id, String username, String status, boolean owner) {
        return new CompanyMemberResponse(id, username, "13800000001", "SHIP_AGENT", status, owner, List.of("SHIP_AGENT"));
    }

    private List<RoleResponse> assignableRoles() {
        return List.of(
            new RoleResponse("SHIP_AGENT", "船代", "SHIP_AGENT"),
            new RoleResponse("SUPPLIER", "供货商", "SUPPLIER")
        );
    }
}
