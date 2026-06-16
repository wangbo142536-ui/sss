USE ship_supply_platform;

SET @schema_name := DATABASE();
SET @table_name := 'shop_sku_import_preview_row';

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN raw_name VARCHAR(1000) NULL AFTER thumbnail_url',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'raw_name'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN raw_spec VARCHAR(1000) NULL AFTER raw_name',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'raw_spec'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN clean_name VARCHAR(500) NULL AFTER raw_spec',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'clean_name'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN parsed_attributes_json JSON NULL AFTER clean_name',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'parsed_attributes_json'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN logic_recommendation_json JSON NULL AFTER parsed_attributes_json',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'logic_recommendation_json'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN model_recommendation_json JSON NULL AFTER logic_recommendation_json',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'model_recommendation_json'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN selected_recommendation_source VARCHAR(30) NULL AFTER model_recommendation_json',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'selected_recommendation_source'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN review_required TINYINT(1) NOT NULL DEFAULT 1 AFTER selected_recommendation_source',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'review_required'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN match_decision VARCHAR(40) NULL AFTER review_required',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'match_decision'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN confirmed_impa_code VARCHAR(80) NULL AFTER match_decision',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'confirmed_impa_code'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN confirmed_platform_code VARCHAR(100) NULL AFTER confirmed_impa_code',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'confirmed_platform_code'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE shop_sku_import_preview_row ADD COLUMN confirmed_recommendation_source VARCHAR(30) NULL AFTER confirmed_platform_code',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = @table_name AND COLUMN_NAME = 'confirmed_recommendation_source'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
