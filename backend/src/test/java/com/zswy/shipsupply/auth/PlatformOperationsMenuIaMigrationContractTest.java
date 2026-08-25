package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PlatformOperationsMenuIaMigrationContractTest {

    private static final Path MIGRATION = Path.of("..", "db", "mysql", "079_platform_operations_menu_ia.sql");

    @Test
    void groupsTheFourRealEntriesUnderPlatformOperationsInTheRequiredOrder() throws Exception {
        assertThat(MIGRATION).exists();
        String sql = Files.readString(MIGRATION);

        assertThat(sql)
            .contains("'PLATFORM_OPERATIONS'")
            .contains("'SHOP_MANAGEMENT'")
            .contains("'SUPPLIER_DATA_ANALYSIS'")
            .contains("'ADMIN_REGISTRATIONS'")
            .contains("'SUPPLIERS'")
            .contains("WHEN 'SHOP_MANAGEMENT' THEN '企业信息'")
            .contains("WHEN 'SUPPLIER_DATA_ANALYSIS' THEN '数据分析'")
            .contains("NULL, 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1")
            .contains("'SUPPLIER_ORDER_VIEW', 'SUPPLIER,PLATFORM_ADMIN', 1")
            .contains("required_permission = VALUES(required_permission)")
            .contains("visible_roles = VALUES(visible_roles)")
            .contains("WHEN 'SHOP_MANAGEMENT' THEN 10")
            .contains("WHEN 'SUPPLIER_DATA_ANALYSIS' THEN 20")
            .contains("WHEN 'ADMIN_REGISTRATIONS' THEN 30")
            .contains("WHEN 'SUPPLIERS' THEN 40")
            .contains("parent_code = 'PLATFORM_OPERATIONS'");
    }

    @Test
    void renamesRegistrationReviewWithoutChangingPermissionOrRoleBindings() throws Exception {
        assertThat(MIGRATION).exists();
        String sql = Files.readString(MIGRATION);

        assertThat(sql)
            .contains("WHEN 'ADMIN_REGISTRATIONS' THEN '服务商审核'")
            .contains("WHEN 'SUPPLIERS' THEN '服务商管理'")
            .doesNotContain("sys_role_permission")
            .doesNotContain("sys_user_menu")
            .doesNotContain("UPDATE sys_permission")
            .doesNotContain("INSERT INTO sys_permission");
    }
}
