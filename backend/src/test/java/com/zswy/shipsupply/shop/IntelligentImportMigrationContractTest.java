package com.zswy.shipsupply.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class IntelligentImportMigrationContractTest {

    @Test
    void migrationAllowsDuplicateSupplierSkusAndAddsSourceRowIdempotency() throws Exception {
        String sql = Files.readString(Path.of("..", "db", "mysql", "071_shop_intelligent_import.sql"));
        assertTrue(sql.contains("DROP INDEX uk_shop_sku_company_type_supplier_code"));
        assertTrue(sql.contains("idx_shop_sku_company_type_supplier_code"));
        assertTrue(sql.contains("shop_sku_import_item"));
        assertTrue(sql.contains("uk_shop_import_item_source(batch_id, source_item_id)"));
        assertTrue(sql.contains("uk_shop_preview_batch_source(batch_id, source_sheet, row_no)"));
        assertFalse(sql.contains("UNIQUE KEY uk_shop_sku_company_type_supplier_code"));
    }

    @Test
    void migrationAddsRealProgressAndBothStandardLibraryLinks() throws Exception {
        String sql = Files.readString(Path.of("..", "db", "mysql", "071_shop_intelligent_import.sql"));
        assertTrue(sql.contains("overall_percent"));
        assertTrue(sql.contains("event_seq"));
        assertTrue(sql.contains("impa_item_id"));
        assertTrue(sql.contains("fk_shop_sku_impa_item"));
        assertTrue(sql.contains("provision_category"));
        assertTrue(sql.contains("material_kind"));
    }
}
