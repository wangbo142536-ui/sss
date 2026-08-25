package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ShopQualitySelectionMigrationContractTest {

    @Test
    void keepsEnterpriseIntroductionAndQualitySelectionInDedicatedStorage() throws Exception {
        String sql = Files.readString(Path.of("..", "db", "mysql", "084_enterprise_introduction_and_sku_quality_selection.sql"));

        assertThat(sql).contains("company_introduction VARCHAR(2000)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS shop_sku_quality_selection");
        assertThat(sql).contains("inspection_time DATETIME NOT NULL");
        assertThat(sql).contains("inspection_content TEXT NOT NULL");
        assertThat(sql).contains("inspection_process TEXT");
        assertThat(sql).contains("inspection_report_file_id VARCHAR(80)");
        assertThat(sql).contains("inspection_conclusion VARCHAR(1000) NOT NULL");
        assertThat(sql).contains("audit_trail_json JSON NOT NULL");
        assertThat(sql).contains("UNIQUE KEY uk_shop_sku_quality_selection_sku (sku_id)");
    }
}
