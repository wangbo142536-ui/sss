package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PlatformAdminUniquenessMigrationContractTest {

    @Test
    void platformAdminRoleAndDirectPlatformMenusBelongOnlyToCanonicalAdmin() throws Exception {
        Path migration = Path.of("../db/mysql/078_unique_platform_admin_role_boundary.sql");
        assertThat(migration).exists();
        String sql = Files.readString(migration);
        assertThat(sql).contains(
            "role.role_code = 'PLATFORM_ADMIN'",
            "role.role_type = 'PLATFORM_ADMIN'",
            "user_account.username <> 'admin'",
            "REPLACE(COALESCE(menu_item.visible_roles, ''), ' ', '') = 'PLATFORM_ADMIN'",
            "user_account.username = 'admin'"
        );
        assertThat(sql).doesNotContain("password_hash", "UPDATE sys_user SET password");
    }
}
