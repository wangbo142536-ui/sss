package com.zswy.shipsupply.shop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SafeNewAmberCatalogCleanupMigrationContractTest {

    @Test
    void migrationBacksUpRowsAndNeverDeletesOrderReferencedHistory() throws Exception {
        String sql = Files.readString(Path.of("..", "db", "mysql", "086_safe_cleanup_new_amber_catalog.sql"));

        assertTrue(sql.contains("shop_sku_safe_backup_20260824"));
        assertTrue(sql.contains("shop_sku_unit_price_safe_backup_20260824"));
        assertTrue(sql.contains("shop_sku_attribute_safe_backup_20260824"));
        assertTrue(sql.contains("SAFE_NEW_AMBER_20260824"));
        assertTrue(sql.contains("EXISTS (\n    SELECT 1\n    FROM purchase_order_item"));
        assertTrue(sql.contains("NOT EXISTS (\n    SELECT 1\n    FROM purchase_order_item"));
        assertTrue(sql.contains("s.shelf_status = 'OFF_SHELF'"));
        assertTrue(sql.contains("category.category_code = LEFT(s.impa_code, 2)"));
        assertTrue(sql.contains("s.specification_summary = NULLIF(TRIM(ii.specification_cn), '')"));
        assertTrue(sql.contains("s.packing = NULL"));
    }
}
