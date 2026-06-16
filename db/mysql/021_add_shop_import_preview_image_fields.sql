-- Shop SKU import preview image fields.
-- Idempotent migration: keeps preview image references for confirm-import.

SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN image_file_id VARCHAR(80) NULL COMMENT ''Imported embedded image file id'' AFTER exception_reason',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku_import_preview_row'
    AND column_name = 'image_file_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN image_url VARCHAR(500) NULL COMMENT ''Imported embedded image URL'' AFTER image_file_id',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku_import_preview_row'
    AND column_name = 'image_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN thumbnail_url VARCHAR(500) NULL COMMENT ''Imported embedded image thumbnail URL'' AFTER image_url',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'shop_sku_import_preview_row'
    AND column_name = 'thumbnail_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
