package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ShopProductDescriptionMigrationContractTest {

    @Test
    void migrationAddsProductDescriptionWithoutDestroyingExistingSkuData() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "072_shop_sku_product_description.sql"));

        assertThat(script).contains("column_name = 'product_description'");
        assertThat(script).contains("ADD COLUMN product_description TEXT NULL AFTER product_name");
        assertThat(script).doesNotContain("DROP TABLE");
        assertThat(script).doesNotContain("TRUNCATE");
        assertThat(script).doesNotContain("DELETE FROM shop_sku");
    }
}
