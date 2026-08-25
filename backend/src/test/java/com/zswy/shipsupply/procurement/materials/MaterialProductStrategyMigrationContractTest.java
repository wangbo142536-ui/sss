package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MaterialProductStrategyMigrationContractTest {

    @Test
    void persistsComparisonSettingsWithoutInventingSystemProductTags() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "081_material_product_strategy.sql"));

        assertThat(script).doesNotContain("system_strategy_tags");
        assertThat(script).doesNotContain("strategy_quality_score");
        assertThat(script).doesNotContain("strategy_price_score");
        assertThat(script).contains("material_comparison_strategy_setting");
        assertThat(script).contains("mixed_supplier_count");
        assertThat(script).contains("core_item_ids_json");
        assertThat(script).contains("product_tags");
        assertThat(script).doesNotContain("UPDATE shop_sku");
    }

    @Test
    void changesOnlyFutureComparisonStrategyDefaultsToThreeSuppliers() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "082_material_comparison_default_supplier_count.sql"));

        assertThat(script).contains("ALTER COLUMN mixed_supplier_count SET DEFAULT 3");
        assertThat(script).doesNotContain("UPDATE material_comparison_strategy_setting");
    }
}
