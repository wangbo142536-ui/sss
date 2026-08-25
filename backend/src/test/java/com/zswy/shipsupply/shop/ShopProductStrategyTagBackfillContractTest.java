package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ShopProductStrategyTagBackfillContractTest {

    @Test
    void importedSkuStrategyTagsUseStableIndependentRatiosWithoutReplacingOtherTags() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "083_imported_sku_strategy_tag_backfill.sql"));

        assertThat(script).contains("ROUND(total_count * 0.80, 0)");
        assertThat(script).contains("ROUND(total_count * 0.70, 0)");
        assertThat(script).contains("PRICE_LOW:");
        assertThat(script).contains("QUALITY_HIGH:");
        assertThat(script).contains("JSON_ARRAY_APPEND");
        assertThat(script).contains("JSON_CONTAINS");
        assertThat(script).contains("import_batch_id IS NOT NULL");
        assertThat(script).doesNotContain("SET sku.product_tags = JSON_ARRAY(");
    }
}
