package com.zswy.shipsupply.standardlibrary.provision;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ProvisionStandardLibraryContractTest {

    @Test
    void exposesEnabledProvisionCategoriesFromTheRealStandardTable() throws Exception {
        Path source = Path.of("src/main/java/com/zswy/shipsupply/standardlibrary/provision/ProvisionCategoryRepository.java");
        assertThat(source).exists();
        String sql = Files.readString(source);
        assertThat(sql).contains(
            "FROM provision_category",
            "enabled = 1",
            "category_code",
            "category_name_cn",
            "CASE",
            "parent_code IS NULL",
            "AS category_level"
        );
        assertThat(sql).doesNotContain("parent_code, level, sort_order");
    }

    @Test
    void publishesProvisionCategoryEndpointAndMenuMigration() throws Exception {
        Path controller = Path.of("src/main/java/com/zswy/shipsupply/standardlibrary/provision/ProvisionCategoryController.java");
        assertThat(controller).exists();
        assertThat(Files.readString(controller)).contains("/api/standard-library/provision/categories");

        Path migration = Path.of("../db/mysql/077_food_standard_library_menu.sql");
        assertThat(migration).exists();
        String sql = Files.readString(migration);
        assertThat(sql).contains("STANDARD_LIBRARY_PROVISION", "STANDARD_LIBRARY_PROVISION_VIEW", "/standard-library/provision", "BASIC_MANAGEMENT");
    }
}
