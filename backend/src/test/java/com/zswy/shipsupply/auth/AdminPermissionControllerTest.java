package com.zswy.shipsupply.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminPermissionController.class)
class AdminPermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminPermissionService adminPermissionService;

    @Test
    void listsAdminPermissionResources() throws Exception {
        when(adminPermissionService.users("Bearer dev-token")).thenReturn(List.of(
            new AdminUserResponse(1L, "13800138000", "13800138000", "SHIP_AGENT", "ACTIVE", "舟山测试船代", List.of("SHIP_AGENT"))
        ));
        when(adminPermissionService.roles("Bearer dev-token")).thenReturn(List.of(
            new RoleResponse("SHIP_AGENT", "船代", "SHIP_AGENT")
        ));
        when(adminPermissionService.menus("Bearer dev-token")).thenReturn(List.of(
            new MenuResponse("DASHBOARD", "Dashboard", "/dashboard", "LayoutDashboard", null, 10, "DASHBOARD_VIEW", "SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN", List.of())
        ));
        when(adminPermissionService.permissions("Bearer dev-token")).thenReturn(List.of(
            new PermissionResponse("DASHBOARD_VIEW", "Dashboard查看", "MENU", "DASHBOARD", "VIEW")
        ));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].phone").value("13800138000"));
        mockMvc.perform(get("/api/admin/roles").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].roleCode").value("SHIP_AGENT"));
        mockMvc.perform(get("/api/admin/menus").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].menuCode").value("DASHBOARD"));
        mockMvc.perform(get("/api/admin/permissions").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].permissionCode").value("DASHBOARD_VIEW"));
    }

    @Test
    void updatesRolePermissionsAndUserRoles() throws Exception {
        when(adminPermissionService.rolePermissions("Bearer dev-token", "SHIP_AGENT")).thenReturn(List.of(
            new PermissionResponse("DASHBOARD_VIEW", "Dashboard查看", "MENU", "DASHBOARD", "VIEW")
        ));

        mockMvc.perform(get("/api/admin/roles/SHIP_AGENT/permissions").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].permissionCode").value("DASHBOARD_VIEW"));

        mockMvc.perform(put("/api/admin/roles/SHIP_AGENT/permissions")
                .header("Authorization", "Bearer dev-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "permissionCodes": ["DASHBOARD_VIEW"]
                    }
                    """))
            .andExpect(status().isNoContent());
        verify(adminPermissionService).updateRolePermissions(eq("Bearer dev-token"), eq("SHIP_AGENT"), any(RolePermissionUpdateRequest.class));

        mockMvc.perform(patch("/api/admin/users/1/roles")
                .header("Authorization", "Bearer dev-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleCodes": ["SHIP_AGENT"]
                    }
                    """))
            .andExpect(status().isNoContent());
        verify(adminPermissionService).updateUserRoles(eq("Bearer dev-token"), eq(1L), any(UserRoleUpdateRequest.class));
    }

    @Test
    void managesRoleMenuPermissions() throws Exception {
        when(adminPermissionService.menuPermissions("Bearer dev-token")).thenReturn(List.of(
            new MenuPermissionResponse(1L, "DASHBOARD", "Dashboard", null, 0, "DASHBOARD_VIEW"),
            new MenuPermissionResponse(2L, "BASIC_MANAGEMENT", "基础管理", null, 30, null),
            new MenuPermissionResponse(3L, "STANDARD_LIBRARY_IMPA", "IMPA标准库", "BASIC_MANAGEMENT", 0, "STANDARD_LIBRARY_IMPA_VIEW")
        ));
        when(adminPermissionService.roleMenuPermissions("Bearer dev-token", "SHIP_AGENT")).thenReturn(new MenuPermissionKeysRequest(
            List.of("DASHBOARD")
        ));

        mockMvc.perform(get("/api/admin/menu-permissions").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].menuId").value(1))
            .andExpect(jsonPath("$[0].menuCode").value("DASHBOARD"))
            .andExpect(jsonPath("$[0].menuName").value("Dashboard"))
            .andExpect(jsonPath("$[0].sortOrder").value(0))
            .andExpect(jsonPath("$[0].routePath").doesNotExist())
            .andExpect(jsonPath("$[1].menuCode").value("BASIC_MANAGEMENT"))
            .andExpect(jsonPath("$[2].parentCode").value("BASIC_MANAGEMENT"));

        mockMvc.perform(get("/api/admin/roles/SHIP_AGENT/menu-permissions").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.menuPermissionKeys[0]").value("DASHBOARD"));

        mockMvc.perform(put("/api/admin/roles/SHIP_AGENT/menu-permissions")
                .header("Authorization", "Bearer dev-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "menuPermissionKeys": ["DASHBOARD", "PROCUREMENT_MATERIALS"]
                    }
                    """))
            .andExpect(status().isNoContent());
        verify(adminPermissionService).updateRoleMenuPermissions(
            eq("Bearer dev-token"),
            eq("SHIP_AGENT"),
            any(MenuPermissionKeysRequest.class)
        );
    }

    @Test
    void managesMenuSortOrder() throws Exception {
        when(adminPermissionService.manageableMenus("Bearer dev-token")).thenReturn(List.of(
            new AdminMenuResponse(1L, "DASHBOARD", "Dashboard", "/dashboard", "LayoutDashboard", null, 0, "DASHBOARD_VIEW", "SHIP_AGENT", true),
            new AdminMenuResponse(2L, "PROCUREMENT_MATERIALS", "物料需求", "/procurement/materials", "PackageSearch", null, 10, "PROCUREMENT_MATERIALS_VIEW", "SHIP_AGENT", true)
        ));

        mockMvc.perform(get("/api/admin/permissions/menus").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].menuId").value(1))
            .andExpect(jsonPath("$[0].menuCode").value("DASHBOARD"))
            .andExpect(jsonPath("$[0].routePath").value("/dashboard"))
            .andExpect(jsonPath("$[0].sortOrder").value(0))
            .andExpect(jsonPath("$[0].enabled").value(true));

        mockMvc.perform(put("/api/admin/permissions/menus/sort-order")
                .header("Authorization", "Bearer dev-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "items": [
                        {"menuCode": "DASHBOARD", "sortOrder": 20},
                        {"menuCode": "PROCUREMENT_MATERIALS", "sortOrder": 0}
                      ]
                    }
                    """))
            .andExpect(status().isNoContent());
        verify(adminPermissionService).updateMenuSortOrder(
            eq("Bearer dev-token"),
            any(MenuSortOrderUpdateRequest.class)
        );
    }

    @Test
    void managesMenuStructureTreeAndVisibility() throws Exception {
        when(adminPermissionService.menuStructure("Bearer dev-token")).thenReturn(List.of(
            new AdminMenuTreeResponse(
                10L,
                "BASIC_MANAGEMENT",
                "基础管理",
                null,
                "Settings",
                null,
                30,
                null,
                "SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN",
                true,
                List.of(new AdminMenuTreeResponse(
                    11L,
                    "MENU_MANAGEMENT",
                    "菜单管理",
                    "/admin/menus",
                    "ListTree",
                    "BASIC_MANAGEMENT",
                    40,
                    "ADMIN_MENU_MANAGE",
                    "SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN",
                    true,
                    List.of()
                ))
            )
        ));

        mockMvc.perform(get("/api/admin/permissions/menus/tree").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].menuCode").value("BASIC_MANAGEMENT"))
            .andExpect(jsonPath("$[0].enabled").value(true))
            .andExpect(jsonPath("$[0].children[0].menuCode").value("MENU_MANAGEMENT"))
            .andExpect(jsonPath("$[0].children[0].routePath").value("/admin/menus"));

        mockMvc.perform(put("/api/admin/permissions/menus/structure")
                .header("Authorization", "Bearer dev-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "items": [
                        {"menuCode": "PROCUREMENT_FOOD", "parentCode": null, "sortOrder": 20, "enabled": false},
                        {"menuCode": "MENU_MANAGEMENT", "parentCode": "BASIC_MANAGEMENT", "sortOrder": 40, "enabled": true}
                      ]
                    }
                    """))
            .andExpect(status().isNoContent());
        verify(adminPermissionService).updateMenuStructure(
            eq("Bearer dev-token"),
            any(MenuStructureUpdateRequest.class)
        );
    }
}
