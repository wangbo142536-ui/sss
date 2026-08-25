package com.zswy.shipsupply.shop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ShopSkuClassificationBackfillMigrationContractTest {

    @Test
    void backfillAuditKeepsBroadCategoryAndExactImpaAsSeparateDecisions() throws Exception {
        String sql = Files.readString(Path.of("..", "db", "mysql", "075_shop_sku_classification_backfill_audit.sql"));

        assertTrue(sql.contains("old_category_code"));
        assertTrue(sql.contains("new_category_code"));
        assertTrue(sql.contains("old_impa_code"));
        assertTrue(sql.contains("new_impa_code"));
        assertTrue(sql.contains("decision_method"));
        assertTrue(sql.contains("confidence_level"));
        assertTrue(sql.contains("UNIQUE KEY uk_sku_classification_backfill_run"));
    }
}
