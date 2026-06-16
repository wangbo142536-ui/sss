package com.zswy.shipsupply.auth;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminPermissionService {

    private final AuthRepository authRepository;
    private final TokenService tokenService;

    public AdminPermissionService(AuthRepository authRepository, TokenService tokenService) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
    }

    public List<AdminUserResponse> users(String authorizationHeader) {
        requirePlatformAdmin(authorizationHeader);
        return authRepository.allUsers();
    }

    public List<RoleResponse> roles(String authorizationHeader) {
        requirePlatformAdmin(authorizationHeader);
        return authRepository.allRoles();
    }

    public List<MenuResponse> menus(String authorizationHeader) {
        requirePlatformAdmin(authorizationHeader);
        return authRepository.allMenus();
    }

    public List<PermissionResponse> permissions(String authorizationHeader) {
        requirePlatformAdmin(authorizationHeader);
        return authRepository.allPermissions();
    }

    public List<MenuPermissionResponse> menuPermissions(String authorizationHeader) {
        requirePlatformAdmin(authorizationHeader);
        return authRepository.allMenuPermissions();
    }

    public List<AdminMenuResponse> manageableMenus(String authorizationHeader) {
        requirePlatformAdmin(authorizationHeader);
        return authRepository.allManageableMenus();
    }

    public List<AdminMenuTreeResponse> menuStructure(String authorizationHeader) {
        requirePlatformAdmin(authorizationHeader);
        return authRepository.buildAdminMenuTree(authRepository.allManageableMenus());
    }

    public List<PermissionResponse> rolePermissions(String authorizationHeader, String roleCode) {
        requirePlatformAdmin(authorizationHeader);
        return authRepository.permissionsForRole(roleCode);
    }

    public MenuPermissionKeysRequest roleMenuPermissions(String authorizationHeader, String roleCode) {
        requirePlatformAdmin(authorizationHeader);
        return new MenuPermissionKeysRequest(authRepository.roleMenuPermissionKeys(roleCode));
    }

    @Transactional
    public void updateRolePermissions(String authorizationHeader, String roleCode, RolePermissionUpdateRequest request) {
        Long operatorUserId = requirePlatformAdmin(authorizationHeader);
        List<String> permissionCodes = request.permissionCodes() == null ? List.of() : request.permissionCodes();
        authRepository.replaceRolePermissions(roleCode, permissionCodes);
        authRepository.log(
            operatorUserId,
            "UPDATE_ROLE_PERMISSIONS",
            "ROLE",
            roleCode,
            "/api/admin/roles/" + roleCode + "/permissions",
            String.join(",", permissionCodes)
        );
    }

    @Transactional
    public void updateRoleMenuPermissions(String authorizationHeader, String roleCode, MenuPermissionKeysRequest request) {
        Long operatorUserId = requirePlatformAdmin(authorizationHeader);
        List<String> menuCodes = request == null || request.menuPermissionKeys() == null
            ? List.of()
            : request.menuPermissionKeys();
        authRepository.replaceRoleMenuPermissions(roleCode, menuCodes);
        authRepository.log(
            operatorUserId,
            "UPDATE_ROLE_MENU_PERMISSIONS",
            "ROLE",
            roleCode,
            "/api/admin/roles/" + roleCode + "/menu-permissions",
            String.join(",", menuCodes)
        );
    }

    @Transactional
    public void updateMenuSortOrder(String authorizationHeader, MenuSortOrderUpdateRequest request) {
        Long operatorUserId = requirePlatformAdmin(authorizationHeader);
        List<MenuSortOrderItem> items = request == null || request.items() == null
            ? List.of()
            : request.items();
        for (MenuSortOrderItem item : items) {
            if (item.menuCode() == null || item.menuCode().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "menuCode is required");
            }
            if (item.sortOrder() == null || item.sortOrder() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SORT_ORDER");
            }
        }
        authRepository.updateMenuSortOrders(items);
        authRepository.log(
            operatorUserId,
            "UPDATE_MENU_SORT_ORDER",
            "MENU",
            "BATCH",
            "/api/admin/permissions/menus/sort-order",
            sortOrderLog(items)
        );
    }

    @Transactional
    public void updateMenuStructure(String authorizationHeader, MenuStructureUpdateRequest request) {
        Long operatorUserId = requirePlatformAdmin(authorizationHeader);
        List<MenuStructureItem> items = request == null || request.items() == null
            ? List.of()
            : request.items();
        List<AdminMenuResponse> existingMenus = authRepository.allManageableMenus();
        List<MenuStructureItem> normalizedItems = normalizeMenuStructureItems(items, existingMenus);
        authRepository.updateMenuStructures(normalizedItems);
        authRepository.log(
            operatorUserId,
            "UPDATE_MENU_STRUCTURE",
            "MENU",
            "BATCH",
            "/api/admin/permissions/menus/structure",
            menuStructureLog(normalizedItems)
        );
    }

    @Transactional
    public void updateUserRoles(String authorizationHeader, Long userId, UserRoleUpdateRequest request) {
        Long operatorUserId = requirePlatformAdmin(authorizationHeader);
        List<String> roleCodes = request.roleCodes() == null ? List.of() : request.roleCodes();
        authRepository.replaceUserRoles(userId, roleCodes);
        authRepository.log(
            operatorUserId,
            "UPDATE_USER_ROLES",
            "USER",
            String.valueOf(userId),
            "/api/admin/users/" + userId + "/roles",
            String.join(",", roleCodes)
        );
    }

    private Long requirePlatformAdmin(String authorizationHeader) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        boolean isAdmin = authRepository.rolesForUser(userId).stream()
            .anyMatch(role -> "PLATFORM_ADMIN".equals(role.roleCode()));
        if (!"ACTIVE".equals(user.status()) || !isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Platform admin role is required"
            );
        }
        return userId;
    }

    private String sortOrderLog(List<MenuSortOrderItem> items) {
        return String.join(
            ",",
            items.stream()
                .map(item -> item.menuCode() + "=" + item.sortOrder())
                .toList()
        );
    }

    private List<MenuStructureItem> normalizeMenuStructureItems(List<MenuStructureItem> items, List<AdminMenuResponse> existingMenus) {
        Set<String> menuCodes = new HashSet<>();
        Map<String, String> parentByCode = new HashMap<>();
        for (AdminMenuResponse menu : existingMenus) {
            menuCodes.add(menu.menuCode());
            parentByCode.put(menu.menuCode(), textOrNull(menu.parentCode()));
        }
        List<MenuStructureItem> normalizedItems = items.stream()
            .map(item -> {
                String menuCode = requiredMenuCode(item.menuCode());
                String parentCode = textOrNull(item.parentCode());
                if (item.sortOrder() == null || item.sortOrder() < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SORT_ORDER");
                }
                if (item.enabled() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "enabled is required");
                }
                if (menuCode.equals(parentCode)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MENU_PARENT_SELF_REFERENCE");
                }
                if (!menuCodes.contains(menuCode)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MENU_NOT_FOUND");
                }
                if (parentCode != null && !menuCodes.contains(parentCode)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MENU_PARENT_NOT_FOUND");
                }
                parentByCode.put(menuCode, parentCode);
                return new MenuStructureItem(menuCode, parentCode, item.sortOrder(), item.enabled());
            })
            .toList();
        validateNoParentCycles(parentByCode);
        return normalizedItems;
    }

    private void validateNoParentCycles(Map<String, String> parentByCode) {
        for (String menuCode : parentByCode.keySet()) {
            Set<String> seen = new HashSet<>();
            String current = menuCode;
            while (current != null) {
                if (!seen.add(current)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MENU_PARENT_CYCLE");
                }
                current = parentByCode.get(current);
            }
        }
    }

    private String requiredMenuCode(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "menuCode is required");
        }
        return value.trim();
    }

    private String textOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String menuStructureLog(List<MenuStructureItem> items) {
        return String.join(
            ",",
            items.stream()
                .map(item -> item.menuCode()
                    + "{parent=" + item.parentCode()
                    + ",sort=" + item.sortOrder()
                    + ",enabled=" + item.enabled()
                    + "}")
                .toList()
        );
    }
}
