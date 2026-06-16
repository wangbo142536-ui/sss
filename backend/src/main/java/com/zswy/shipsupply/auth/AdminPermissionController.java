package com.zswy.shipsupply.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    public AdminPermissionController(AdminPermissionService adminPermissionService) {
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminPermissionService.users(authorizationHeader);
    }

    @GetMapping("/roles")
    public List<RoleResponse> roles(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminPermissionService.roles(authorizationHeader);
    }

    @GetMapping("/menus")
    public List<MenuResponse> menus(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminPermissionService.menus(authorizationHeader);
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> permissions(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminPermissionService.permissions(authorizationHeader);
    }

    @GetMapping("/menu-permissions")
    public List<MenuPermissionResponse> menuPermissions(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminPermissionService.menuPermissions(authorizationHeader);
    }

    @GetMapping("/permissions/menus")
    public List<AdminMenuResponse> manageableMenus(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminPermissionService.manageableMenus(authorizationHeader);
    }

    @GetMapping("/permissions/menus/tree")
    public List<AdminMenuTreeResponse> menuStructure(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return adminPermissionService.menuStructure(authorizationHeader);
    }

    @PutMapping("/permissions/menus/sort-order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMenuSortOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody MenuSortOrderUpdateRequest request
    ) {
        adminPermissionService.updateMenuSortOrder(authorizationHeader, request);
    }

    @PutMapping("/permissions/menus/structure")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMenuStructure(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody MenuStructureUpdateRequest request
    ) {
        adminPermissionService.updateMenuStructure(authorizationHeader, request);
    }

    @GetMapping("/roles/{roleCode}/permissions")
    public List<PermissionResponse> rolePermissions(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable String roleCode
    ) {
        return adminPermissionService.rolePermissions(authorizationHeader, roleCode);
    }

    @PutMapping("/roles/{roleCode}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRolePermissions(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable String roleCode,
        @RequestBody RolePermissionUpdateRequest request
    ) {
        adminPermissionService.updateRolePermissions(authorizationHeader, roleCode, request);
    }

    @GetMapping("/roles/{roleCode}/menu-permissions")
    public MenuPermissionKeysRequest roleMenuPermissions(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable String roleCode
    ) {
        return adminPermissionService.roleMenuPermissions(authorizationHeader, roleCode);
    }

    @PutMapping("/roles/{roleCode}/menu-permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoleMenuPermissions(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable String roleCode,
        @RequestBody MenuPermissionKeysRequest request
    ) {
        adminPermissionService.updateRoleMenuPermissions(authorizationHeader, roleCode, request);
    }

    @PatchMapping("/users/{userId}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUserRoles(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId,
        @RequestBody UserRoleUpdateRequest request
    ) {
        adminPermissionService.updateUserRoles(authorizationHeader, userId, request);
    }
}
