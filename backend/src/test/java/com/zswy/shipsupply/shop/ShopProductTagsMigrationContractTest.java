package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ShopProductTagsMigrationContractTest {

    @Test
    void migrationAddsJsonProductTagsWithoutDestroyingSkuData() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "073_shop_sku_product_tags.sql"));

        assertThat(script).contains("column_name = 'product_tags'");
        assertThat(script).contains("ADD COLUMN product_tags JSON NULL AFTER product_description");
        assertThat(script).doesNotContain("DROP TABLE");
        assertThat(script).doesNotContain("TRUNCATE");
        assertThat(script).doesNotContain("DELETE FROM shop_sku");
    }
}
