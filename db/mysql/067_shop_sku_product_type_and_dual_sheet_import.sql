-- Isolate material and food SKUs while keeping one shop_sku table.
-- The migration is idempotent for environments that replay SQL files manually.

SET @schema_name := DATABASE();

SET @has_source_sheet := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku_import_preview_row'
    AND column_name = 'source_sheet'
);
SET @sql := IF(
  @has_source_sheet = 0,
  'ALTER TABLE shop_sku_import_preview_row ADD COLUMN source_sheet VARCHAR(30) NULL AFTER product_type',
  'SELECT ''shop_sku_import_preview_row.source_sheet already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE shop_sku_import_preview_row
SET source_sheet = CASE WHEN product_type = 'FOOD' THEN '伙食' ELSE '物料' END
WHERE source_sheet IS NULL OR source_sheet = '';

SET @has_confirmed_at := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku_import_preview_row'
    AND column_name = 'confirmed_at'
);
SET @sql := IF(
  @has_confirmed_at = 0,
  'ALTER TABLE shop_sku_import_preview_row ADD COLUMN confirmed_at DATETIME NULL AFTER candidate_snapshot_json',
  'SELECT ''shop_sku_import_preview_row.confirmed_at already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_old_sku_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku'
    AND index_name = 'uk_shop_sku_company_supplier_code'
);
SET @sql := IF(
  @has_old_sku_unique > 0,
  'ALTER TABLE shop_sku DROP INDEX uk_shop_sku_company_supplier_code',
  'SELECT ''old shop_sku unique index already removed'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_typed_sku_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku'
    AND index_name = 'uk_shop_sku_company_type_supplier_code'
);
SET @sql := IF(
  @has_typed_sku_unique = 0,
  'ALTER TABLE shop_sku ADD UNIQUE KEY uk_shop_sku_company_type_supplier_code (company_id, product_type, supplier_sku_code)',
  'SELECT ''typed shop_sku unique index already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_preview_sheet_row_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku_import_preview_row'
    AND index_name = 'uk_shop_preview_batch_type_row'
);
SET @sql := IF(
  @has_preview_sheet_row_unique = 0,
  'ALTER TABLE shop_sku_import_preview_row ADD UNIQUE KEY uk_shop_preview_batch_type_row (batch_id, product_type, row_no)',
  'SELECT ''preview sheet row unique index already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
