package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminPermissionServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private TokenService tokenService;

    private AdminPermissionService service;

    @BeforeEach
    void setUp() {
        service = new AdminPermissionService(authRepository, tokenService);
    }

    @Test
    void savesMenuPermissionsWithoutReplacingAllRolePermissions() {
        when(tokenService.requireUserId("Bearer admin-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", "13800000000", "hash", "PLATFORM_ADMIN", "ACTIVE", 1L)
        );
        when(authRepository.rolesForUser(1L)).thenReturn(List.of(
            new RoleResponse("PLATFORM_ADMIN", "平台管理员", "PLATFORM_ADMIN")
        ));

        service.updateRoleMenuPermissions(
            "Bearer admin-token",
            "SHIP_AGENT",
            new MenuPermissionKeysRequest(List.of("DASHBOARD", "PROCUREMENT_MATERIALS"))
        );

        verify(authRepository).replaceRoleMenuPermissions("SHIP_AGENT", List.of("DASHBOARD", "PROCUREMENT_MATERIALS"));
        verify(authRepository).log(
            1L,
            "UPDATE_ROLE_MENU_PERMISSIONS",
            "ROLE",
            "SHIP_AGENT",
            "/api/admin/roles/SHIP_AGENT/menu-permissions",
            "DASHBOARD,PROCUREMENT_MATERIALS"
        );
    }

    @Test
    void listsMenuPermissionsAndSelectedRoleMenuCodes() {
        when(tokenService.requireUserId("Bearer admin-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", "13800000000", "hash", "PLATFORM_ADMIN", "ACTIVE", 1L)
        );
        when(authRepository.rolesForUser(1L)).thenReturn(List.of(
            new RoleResponse("PLATFORM_ADMIN", "平台管理员", "PLATFORM_ADMIN")
        ));
        when(authRepository.allMenuPermissions()).thenReturn(List.of(
            new MenuPermissionResponse(1L, "DASHBOARD", "Dashboard", null, 0, "DASHBOARD_VIEW")
        ));
        when(authRepository.roleMenuPermissionKeys("SHIP_AGENT")).thenReturn(List.of("DASHBOARD"));

        assertThat(service.menuPermissions("Bearer admin-token")).hasSize(1);
        assertThat(service.roleMenuPermissions("Bearer admin-token", "SHIP_AGENT").menuPermissionKeys())
            .containsExactly("DASHBOARD");
    }

    @Test
    void updatesOnlySubmittedMenuSortOrderItems() {
        when(tokenService.requireUserId("Bearer admin-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", "13800000000", "hash", "PLATFORM_ADMIN", "ACTIVE", 1L)
        );
        when(authRepository.rolesForUser(1L)).thenReturn(List.of(
            new RoleResponse("PLATFORM_ADMIN", "平台管理员", "PLATFORM_ADMIN")
        ));

        service.updateMenuSortOrder(
            "Bearer admin-token",
            new MenuSortOrderUpdateRequest(List.of(
                new MenuSortOrderItem("DASHBOARD", 20),
                new MenuSortOrderItem("PROCUREMENT_MATERIALS", 0)
            ))
        );

        verify(authRepository).updateMenuSortOrders(List.of(
            new MenuSortOrderItem("DASHBOARD", 20),
            new MenuSortOrderItem("PROCUREMENT_MATERIALS", 0)
        ));
        verify(authRepository).log(
            1L,
            "UPDATE_MENU_SORT_ORDER",
            "MENU",
            "BATCH",
            "/api/admin/permissions/menus/sort-order",
            "DASHBOARD=20,PROCUREMENT_MATERIALS=0"
        );
    }

    @Test
    void returnsMenusInUpdatedSortOrderAfterSavingSortOrder() {
        when(tokenService.requireUserId("Bearer admin-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", "13800000000", "hash", "PLATFORM_ADMIN", "ACTIVE", 1L)
        );
        when(authRepository.rolesForUser(1L)).thenReturn(List.of(
            new RoleResponse("PLATFORM_ADMIN", "平台管理员", "PLATFORM_ADMIN")
        ));
        when(authRepository.allManageableMenus()).thenReturn(List.of(
            new AdminMenuResponse(2L, "PROCUREMENT_MATERIALS", "物料需求", "/procurement/materials", "PackageSearch", null, 0, "PROCUREMENT_MATERIALS_VIEW", "SHIP_AGENT", true),
            new AdminMenuResponse(1L, "DASHBOARD", "Dashboard", "/dashboard", "LayoutDashboard", null, 20, "DASHBOARD_VIEW", "SHIP_AGENT", true)
        ));

        service.updateMenuSortOrder(
            "Bearer admin-token",
            new MenuSortOrderUpdateRequest(List.of(
                new MenuSortOrderItem("DASHBOARD", 20),
                new MenuSortOrderItem("PROCUREMENT_MATERIALS", 0)
            ))
        );

        assertThat(service.manageableMenus("Bearer admin-token"))
            .extracting(AdminMenuResponse::menuCode)
            .containsExactly("PROCUREMENT_MATERIALS", "DASHBOARD");
    }

    @Test
    void rejectsNegativeMenuSortOrder() {
        when(tokenService.requireUserId("Bearer admin-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", "13800000000", "hash", "PLATFORM_ADMIN", "ACTIVE", 1L)
        );
        when(authRepository.rolesForUser(1L)).thenReturn(List.of(
            new RoleResponse("PLATFORM_ADMIN", "平台管理员", "PLATFORM_ADMIN")
        ));

        assertThatThrownBy(() -> service.updateMenuSortOrder(
            "Bearer admin-token",
            new MenuSortOrderUpdateRequest(List.of(new MenuSortOrderItem("DASHBOARD", -1)))
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST")
            .hasMessageContaining("INVALID_SORT_ORDER");
    }

    @Test
    void updatesMenuStructureWithoutChangingPermissions() {
        when(tokenService.requireUserId("Bearer admin-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", "13800000000", "hash", "PLATFORM_ADMIN", "ACTIVE", 1L)
        );
        when(authRepository.rolesForUser(1L)).thenReturn(List.of(
            new RoleResponse("PLATFORM_ADMIN", "平台管理员", "PLATFORM_ADMIN")
        ));
        when(authRepository.allManageableMenus()).thenReturn(List.of(
            new AdminMenuResponse(1L, "BASIC_MANAGEMENT", "基础管理", null, "Settings", null, 30, null, "SHIP_AGENT", true),
            new AdminMenuResponse(2L, "MENU_MANAGEMENT", "菜单管理", "/admin/menus", "ListTree", "BASIC_MANAGEMENT", 40, "ADMIN_MENU_MANAGE", "SHIP_AGENT", true),
            new AdminMenuResponse(3L, "PROCUREMENT_FOOD", "伙食采购入口", "/procurement/food", "Utensils", null, 20, "PROCUREMENT_FOOD_VIEW", "SHIP_AGENT", true)
        ));

        List<MenuStructureItem> items = List.of(
            new MenuStructureItem("PROCUREMENT_FOOD", null, 20, false),
            new MenuStructureItem("MENU_MANAGEMENT", "BASIC_MANAGEMENT", 40, true)
        );
        service.updateMenuStructure("Bearer admin-token", new MenuStructureUpdateRequest(items));

        verify(authRepository).updateMenuStructures(items);
        verify(authRepository, never()).replaceRoleMenuPermissions("SHIP_AGENT", List.of());
        verify(authRepository).log(
            1L,
            "UPDATE_MENU_STRUCTURE",
            "MENU",
            "BATCH",
            "/api/admin/permissions/menus/structure",
            "PROCUREMENT_FOOD{parent=null,sort=20,enabled=false},MENU_MANAGEMENT{parent=BASIC_MANAGEMENT,sort=40,enabled=true}"
        );
    }

    @Test
    void rejectsMenuStructureSelfParent() {
        when(tokenService.requireUserId("Bearer admin-token")).thenReturn(1L);
        when(authRepository.getUserById(1L)).thenReturn(
            new AuthenticatedUser(1L, "admin", "13800000000", "hash", "PLATFORM_ADMIN", "ACTIVE", 1L)
        );
        when(authRepository.rolesForUser(1L)).thenReturn(List.of(
            new RoleResponse("PLATFORM_ADMIN", "平台管理员", "PLATFORM_ADMIN")
        ));

        assertThatThrownBy(() -> service.updateMenuStructure(
            "Bearer admin-token",
            new MenuStructureUpdateRequest(List.of(
                new MenuStructureItem("MENU_MANAGEMENT", "MENU_MANAGEMENT", 40, true)
            ))
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST")
            .hasMessageContaining("MENU_PARENT_SELF_REFERENCE");
    }
}
