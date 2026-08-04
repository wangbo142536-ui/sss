package com.zswy.shipsupply.auth;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyMemberService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";

    private final AuthRepository authRepository;
    private final TokenService tokenService;
    private final PasswordHasher passwordHasher;

    public CompanyMemberService(AuthRepository authRepository, TokenService tokenService, PasswordHasher passwordHasher) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
        this.passwordHasher = passwordHasher;
    }

    public CompanyMembersResponse members(String authorizationHeader, String status, String keyword) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        String safeStatus = normalizeStatus(status == null || status.isBlank() ? ACTIVE : status, true);
        return new CompanyMembersResponse(authRepository.companyMembers(manager.companyId(), safeStatus, keyword));
    }

    @Transactional
    public CompanyMemberResponse createMember(String authorizationHeader, CompanyMemberCreateRequest request) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        String username = required(request.username(), "username");
        String name = optionalText(request.name());
        String phone = optionalText(request.phone());
        String email = optionalText(request.email());
        String password = required(request.password(), "password");
        if (authRepository.userExists(username, phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ACCOUNT_EXISTS");
        }
        long userId = authRepository.insertCompanyMember(
            manager.companyId(),
            username,
            name,
            phone,
            email,
            passwordHasher.hash(password),
            manager.userType(),
            manager.id()
        );
        authRepository.replaceCompanyUserRoles(userId, manager.companyId(), normalizedRoles(firstRoles(request.roles(), request.roleCodes()), manager.companyId()));
        authRepository.replaceUserMenuPermissions(userId, normalizedMenuCodes(request.menuPermissionKeys(), manager.companyId()));
        authRepository.log(manager.id(), "CREATE_COMPANY_MEMBER", "USER", String.valueOf(userId), "/api/company/members", username);
        return member(manager.companyId(), userId);
    }

    @Transactional
    public CompanyMemberResponse updateMember(String authorizationHeader, Long userId, CompanyMemberUpdateRequest request) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        CompanyMemberResponse target = requireCompanyMember(manager.companyId(), userId);
        authRepository.updateMemberProfile(userId, optionalText(request.name()), optionalText(request.phone()), optionalText(request.email()));
        if (!target.isCompanyOwner()) {
            authRepository.replaceCompanyUserRoles(userId, manager.companyId(), normalizedRoles(firstRoles(request.roles(), request.roleCodes()), manager.companyId()));
        }
        authRepository.log(manager.id(), "UPDATE_COMPANY_MEMBER", "USER", String.valueOf(userId), "/api/company/members/" + userId, "profile/roles");
        return member(manager.companyId(), userId);
    }

    @Transactional
    public CompanyMemberResponse updateMemberStatus(String authorizationHeader, Long userId, CompanyMemberStatusRequest request) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        CompanyMemberResponse target = requireCompanyMember(manager.companyId(), userId);
        if (target.isCompanyOwner()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COMPANY_OWNER_CANNOT_BE_DISABLED");
        }
        String status = normalizeStatus(request.status(), false);
        authRepository.updateMemberStatus(userId, status);
        if (DISABLED.equals(status)) {
            tokenService.invalidateUser(userId);
        }
        authRepository.log(manager.id(), "UPDATE_COMPANY_MEMBER_STATUS", "USER", String.valueOf(userId), "/api/company/members/" + userId + "/status", status);
        return member(manager.companyId(), userId);
    }

    @Transactional
    public void resetMemberPassword(String authorizationHeader, Long userId, CompanyMemberResetPasswordRequest request) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        requireCompanyMember(manager.companyId(), userId);
        String password = required(request.newPassword(), "newPassword");
        authRepository.updatePassword(userId, passwordHasher.hash(password));
        tokenService.invalidateUser(userId);
        authRepository.log(manager.id(), "RESET_COMPANY_MEMBER_PASSWORD", "USER", String.valueOf(userId), "/api/company/members/" + userId + "/reset-password", "reset");
    }

    @Transactional
    public void updateMemberRoles(String authorizationHeader, Long userId, CompanyMemberRolesRequest request) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        CompanyMemberResponse target = requireCompanyMember(manager.companyId(), userId);
        if (target.isCompanyOwner()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COMPANY_OWNER_ROLES_CANNOT_BE_CHANGED_HERE");
        }
        List<String> roles = normalizedRoles(request.roles(), manager.companyId());
        authRepository.replaceCompanyUserRoles(userId, manager.companyId(), roles);
        authRepository.log(manager.id(), "UPDATE_COMPANY_MEMBER_ROLES", "USER", String.valueOf(userId), "/api/company/members/" + userId + "/roles", String.join(",", roles));
    }

    public CompanyRolesResponse availableRoles(String authorizationHeader) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        return new CompanyRolesResponse(authRepository.companyAssignableRoles(manager.companyId()));
    }

    @Transactional
    public RoleResponse createRole(String authorizationHeader, CompanyRoleUpsertRequest request) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        String roleName = required(request.roleName(), "roleName");
        String roleType = optionalText(request.roleType()) == null ? "CUSTOM" : optionalText(request.roleType()).toUpperCase(Locale.ROOT);
        List<String> menuCodes = normalizedMenuCodes(request.menuPermissionKeys(), manager.companyId());
        String roleCode = authRepository.insertCompanyRole(manager.companyId(), request.roleCode(), roleName, roleType);
        authRepository.replaceRoleMenuPermissions(manager.companyId(), roleCode, menuCodes);
        authRepository.log(manager.id(), "CREATE_COMPANY_ROLE", "ROLE", roleCode, "/api/company/roles", roleName);
        return role(manager.companyId(), roleCode);
    }

    @Transactional
    public RoleResponse updateRole(String authorizationHeader, String roleCode, CompanyRoleUpsertRequest request) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        requireCompanyRole(manager.companyId(), normalizedRoleCode);
        String roleName = required(request.roleName(), "roleName");
        String roleType = optionalText(request.roleType()) == null ? "CUSTOM" : optionalText(request.roleType()).toUpperCase(Locale.ROOT);
        authRepository.updateCompanyRole(manager.companyId(), normalizedRoleCode, roleName, roleType);
        authRepository.replaceRoleMenuPermissions(
            manager.companyId(),
            normalizedRoleCode,
            normalizedMenuCodes(request.menuPermissionKeys(), manager.companyId())
        );
        authRepository.log(manager.id(), "UPDATE_COMPANY_ROLE", "ROLE", normalizedRoleCode, "/api/company/roles/" + normalizedRoleCode, roleName);
        return role(manager.companyId(), normalizedRoleCode);
    }

    @Transactional
    public void deleteRole(String authorizationHeader, String roleCode) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        requireCompanyRole(manager.companyId(), normalizedRoleCode);
        if (normalizedRoleCode.startsWith("COMPANY_ADMIN_")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COMPANY_ADMIN_ROLE_CANNOT_BE_DELETED");
        }
        if (authRepository.companyRoleInUse(manager.companyId(), normalizedRoleCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ROLE_IN_USE");
        }
        authRepository.disableCompanyRole(manager.companyId(), normalizedRoleCode);
        authRepository.log(manager.id(), "DELETE_COMPANY_ROLE", "ROLE", normalizedRoleCode, "/api/company/roles/" + normalizedRoleCode, "disabled");
    }

    public List<MenuResponse> menuPermissions(String authorizationHeader) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        return authRepository.buildTree(authRepository.companyPermissionMenus(manager.companyId()));
    }

    public MenuPermissionKeysRequest memberMenuPermissions(String authorizationHeader, Long userId) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        requireCompanyMember(manager.companyId(), userId);
        return new MenuPermissionKeysRequest(authRepository.userMenuPermissionKeys(userId));
    }

    @Transactional
    public void updateMemberMenuPermissions(String authorizationHeader, Long userId, MenuPermissionKeysRequest request) {
        AuthenticatedUser manager = requireActiveCompanyManager(authorizationHeader);
        CompanyMemberResponse target = requireCompanyMember(manager.companyId(), userId);
        if (target.isCompanyOwner()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COMPANY_OWNER_MENU_PERMISSIONS_CANNOT_BE_CHANGED_HERE");
        }
        List<String> menuCodes = normalizedMenuCodes(request.menuPermissionKeys(), manager.companyId());
        authRepository.replaceUserMenuPermissions(userId, menuCodes);
        authRepository.log(manager.id(), "UPDATE_COMPANY_MEMBER_MENU_PERMISSIONS", "USER", String.valueOf(userId), "/api/company/members/" + userId + "/menu-permissions", String.join(",", menuCodes));
    }

    private AuthenticatedUser requireActiveCompanyManager(String authorizationHeader) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        CompanyResponse company = authRepository.getCompany(user.companyId());
        boolean owner = authRepository.isCompanyOwner(userId);
        if (!ACTIVE.equals(user.status()) || !ACTIVE.equals(company.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "COMPANY_NOT_ACTIVE");
        }
        boolean companyAdmin = owner || authRepository.rolesForUser(userId).stream()
            .anyMatch(role -> "COMPANY_ADMIN".equals(role.roleType()) || role.roleCode().startsWith("COMPANY_ADMIN_"));
        if (!owner && !companyAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_COMPANY_ADMIN");
        }
        return user;
    }

    private CompanyMemberResponse requireCompanyMember(Long companyId, Long userId) {
        return authRepository.findCompanyMember(companyId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND"));
    }

    private CompanyMemberResponse member(Long companyId, Long userId) {
        CompanyMemberResponse member = requireCompanyMember(companyId, userId);
        return new CompanyMemberResponse(
            member.userId(),
            member.username(),
            member.name(),
            member.phone(),
            member.email(),
            member.userType(),
            member.status(),
            member.isCompanyOwner(),
            authRepository.roleCodesForUser(userId),
            authRepository.userMenuPermissionKeys(userId)
        );
    }

    private RoleResponse role(long companyId, String roleCode) {
        return authRepository.companyAssignableRoles(companyId).stream()
            .filter(role -> role.roleCode().equals(roleCode))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND"));
    }

    private void requireCompanyRole(long companyId, String roleCode) {
        if (!authRepository.companyRoleExists(companyId, roleCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND");
        }
    }

    private List<String> normalizedRoles(List<String> roles, long companyId) {
        List<String> normalized = roles == null ? List.of() : roles.stream()
            .filter(role -> role != null && !role.isBlank())
            .map(this::normalizeRoleCode)
            .toList();
        List<String> assignable = authRepository.companyAssignableRoles(companyId).stream()
            .map(RoleResponse::roleCode)
            .toList();
        for (String role : normalized) {
            if (!assignable.contains(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_ROLE");
            }
        }
        return normalized;
    }

    private List<String> normalizedMenuCodes(List<String> menuCodes, long companyId) {
        List<String> normalized = menuCodes == null ? List.of() : menuCodes.stream()
            .filter(code -> code != null && !code.isBlank())
            .map(this::normalizeRoleCode)
            .distinct()
            .toList();
        if (normalized.isEmpty()) {
            return normalized;
        }
        List<String> allowed = authRepository.companyMenuCodes(companyId);
        for (String menuCode : normalized) {
            if (!allowed.contains(menuCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_MENU_PERMISSION");
            }
        }
        return normalized;
    }

    private List<String> firstRoles(List<String> roles, List<String> roleCodes) {
        return roleCodes != null ? roleCodes : roles;
    }

    private String normalizeStatus(String value, boolean allowAll) {
        String status = required(value, "status").toUpperCase(Locale.ROOT);
        if (ACTIVE.equals(status) || DISABLED.equals(status) || (allowAll && "ALL".equals(status))) {
            return status;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_STATUS");
    }

    private String normalizeRoleCode(String roleCode) {
        return required(roleCode, "roleCode").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
