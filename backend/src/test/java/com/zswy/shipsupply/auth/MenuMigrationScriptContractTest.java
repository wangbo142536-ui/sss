package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MenuMigrationScriptContractTest {

    @Test
    void companyMemberBoundaryMigrationGroupsMenuAndRevokesPlatformAdministratorAccess() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "069_company_member_menu_and_platform_boundary.sql"));

        assertThat(script).contains("menu_code = 'COMPANY_MEMBERS'");
        assertThat(script).contains("parent_code = 'BASIC_MANAGEMENT'");
        assertThat(script).contains("permission.permission_code = 'COMPANY_MEMBERS_VIEW'");
        assertThat(script).contains("role.role_code = 'PLATFORM_ADMIN'");
        assertThat(script).contains("DELETE role_permission");
        assertThat(script).doesNotContain("TRUNCATE");
    }

    @Test
    void inquiriesMenuMigrationCreatesMenuAndBackfillsAdminPermissions() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "014_create_inquiries_menu.sql"));

        assertThat(script).contains("INQUIRY_VIEW");
        assertThat(script).contains("'INQUIRIES', '询价单', '/inquiries'");
        assertThat(script).contains("'FileSearch'");
        assertThat(script).contains("'INQUIRIES', 'VIEW'");
        assertThat(script).contains("NULL, 40");
        assertThat(script).contains("role_type = 'COMPANY_ADMIN'");
        assertThat(script).contains("'PLATFORM_ADMIN'");
        assertThat(script).doesNotContain("DELETE FROM sys_role_permission");
        assertThat(script).doesNotContain("TRUNCATE");
    }

    @Test
    void procurementMenuRestructureMigrationCreatesGroupedMenusAndBackfillsAdminPermissions() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "015_restructure_procurement_menus.sql"));

        assertThat(script).contains("MATERIAL_PROCUREMENT_GROUP");
        assertThat(script).contains("FOOD_PROCUREMENT_GROUP");
        assertThat(script).contains("'MATERIAL_PROCUREMENT_GROUP', '物料采购', '', 'PackageSearch', NULL, 10");
        assertThat(script).contains("'FOOD_PROCUREMENT_GROUP', '伙食采购管理', '', 'Utensils', NULL, 20");
        assertThat(script).contains("'PROCUREMENT_MATERIALS', '物料采购入口', '/procurement/materials'");
        assertThat(script).contains("'INQUIRIES', '询价管理', '/inquiries'");
        assertThat(script).contains("'QUOTES', '报价管理', '/quotes'");
        assertThat(script).contains("'COMPARISON', '比价管理', '/comparison'");
        assertThat(script).contains("'ORDERS', '采购管理', '/orders'");
        assertThat(script).contains("'PROCUREMENT_FOOD', '伙食采购入口', '/procurement/food'");
        assertThat(script).contains("'FOOD_INQUIRIES', '询价管理', '/food/inquiries'");
        assertThat(script).contains("'FOOD_QUOTES', '报价管理', '/food/quotes'");
        assertThat(script).contains("'FOOD_COMPARISON', '比价管理', '/food/comparison'");
        assertThat(script).contains("'FOOD_ORDERS', '采购管理', '/food/orders'");
        assertThat(script).contains("INSERT IGNORE INTO sys_role_permission");
        assertThat(script).contains("role.role_type = 'COMPANY_ADMIN'");
        assertThat(script).contains("'PLATFORM_ADMIN'");
        assertThat(script).doesNotContain("DELETE FROM sys_role_permission");
        assertThat(script).doesNotContain("TRUNCATE");
    }

    @Test
    void crewServiceAndRenamedOperationsMigrationUpdatesMenusAndBackfillsAdminPermissions() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "016_crew_service_and_operations_menu.sql"));

        assertThat(script).contains("CREW_SERVICE_VIEW");
        assertThat(script).contains("'CREW_SERVICE', '船员服务', '/crew-services'");
        assertThat(script).contains("NULL, 30");
        assertThat(script).contains("menu_code = 'DELIVERY_TASKS'");
        assertThat(script).contains("menu_name = '驳船管理'");
        assertThat(script).contains("menu_code = 'SETTLEMENT'");
        assertThat(script).contains("menu_name = '结算管理'");
        assertThat(script).contains("INSERT IGNORE INTO sys_role_permission");
        assertThat(script).contains("role.role_type = 'COMPANY_ADMIN'");
        assertThat(script).contains("'PLATFORM_ADMIN'");
        assertThat(script).doesNotContain("DELETE FROM sys_role_permission");
        assertThat(script).doesNotContain("TRUNCATE");
    }
}
