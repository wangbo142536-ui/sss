-- Intelligent shop import: source-row identity, real progress and standard-library linkage.
-- Idempotent migration. Existing business rows are preserved.

SET @schema_name := DATABASE();

CREATE TABLE IF NOT EXISTS provision_category (
  id BIGINT NOT NULL AUTO_INCREMENT,
  category_code VARCHAR(40) NOT NULL,
  category_name_cn VARCHAR(120) NOT NULL,
  category_name_en VARCHAR(160) NULL,
  parent_code VARCHAR(40) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_provision_category_code (category_code),
  KEY idx_provision_category_parent (parent_code),
  KEY idx_provision_category_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Provision standard category library';

INSERT INTO provision_category(category_code, category_name_cn, category_name_en, sort_order)
VALUES
  ('PROVISION_FRESH', '生鲜食品', 'Fresh food', 10),
  ('PROVISION_GRAIN_OIL', '粮油米面', 'Grain and edible oil', 20),
  ('PROVISION_SEASONING', '调味品', 'Seasoning', 30),
  ('PROVISION_DRY_FOOD', '干货及罐头', 'Dry and canned food', 40),
  ('PROVISION_BEVERAGE', '饮料及乳制品', 'Beverage and dairy', 50),
  ('PROVISION_FROZEN', '冷冻食品', 'Frozen food', 60),
  ('PROVISION_OTHER', '其他伙食', 'Other provisions', 99)
ON DUPLICATE KEY UPDATE category_name_cn = VALUES(category_name_cn), category_name_en = VALUES(category_name_en), sort_order = VALUES(sort_order);

SET @has_old_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name AND table_name = 'shop_sku'
    AND index_name = 'uk_shop_sku_company_type_supplier_code'
);
SET @sql := IF(@has_old_unique > 0,
  'ALTER TABLE shop_sku DROP INDEX uk_shop_sku_company_type_supplier_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_supplier_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name AND table_name = 'shop_sku'
    AND index_name = 'idx_shop_sku_company_type_supplier_code'
);
SET @sql := IF(@has_supplier_index = 0,
  'ALTER TABLE shop_sku ADD KEY idx_shop_sku_company_type_supplier_code(company_id, product_type, supplier_sku_code)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_impa_item_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'shop_sku' AND column_name = 'impa_item_id'
);
SET @sql := IF(@has_impa_item_id = 0,
  'ALTER TABLE shop_sku ADD COLUMN impa_item_id BIGINT NULL AFTER impa_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_library_type := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'shop_sku' AND column_name = 'standard_library_type'
);
SET @sql := IF(@has_library_type = 0,
  'ALTER TABLE shop_sku ADD COLUMN standard_library_type VARCHAR(30) NULL AFTER product_type, ADD COLUMN standard_category_code VARCHAR(50) NULL AFTER standard_library_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_material_kind := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'shop_sku' AND column_name = 'material_kind'
);
SET @sql := IF(@has_material_kind = 0,
  'ALTER TABLE shop_sku ADD COLUMN material_kind VARCHAR(30) NULL AFTER product_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE shop_sku sku
JOIN impa_item item ON item.impa_code = sku.impa_code
SET sku.impa_item_id = item.id,
    sku.standard_library_type = 'IMPA',
    sku.standard_category_code = item.category_code
WHERE sku.product_type = 'MATERIAL' AND sku.impa_item_id IS NULL;
UPDATE shop_sku SET standard_library_type = 'PROVISION' WHERE product_type = 'FOOD' AND standard_library_type IS NULL;

ALTER TABLE shop_sku MODIFY COLUMN impa_item_id BIGINT UNSIGNED NULL;

SET @has_impa_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name AND table_name = 'shop_sku' AND index_name = 'idx_shop_sku_impa_item'
);
SET @sql := IF(@has_impa_index = 0,
  'ALTER TABLE shop_sku ADD KEY idx_shop_sku_impa_item(impa_item_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_impa_fk := (
  SELECT COUNT(*) FROM information_schema.referential_constraints
  WHERE BINARY constraint_schema = BINARY @schema_name
    AND BINARY table_name = BINARY 'shop_sku'
    AND BINARY constraint_name = BINARY 'fk_shop_sku_impa_item'
);
SET @sql := IF(@has_impa_fk = 0,
  'ALTER TABLE shop_sku ADD CONSTRAINT fk_shop_sku_impa_item FOREIGN KEY(impa_item_id) REFERENCES impa_item(id) ON DELETE RESTRICT',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_job_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'shop_sku_import_batch' AND column_name = 'job_id'
);
SET @sql := IF(@has_job_id = 0,
  'ALTER TABLE shop_sku_import_batch ADD COLUMN job_id VARCHAR(64) NULL AFTER id, ADD COLUMN stage VARCHAR(40) NULL AFTER status, ADD COLUMN overall_percent INT NOT NULL DEFAULT 0 AFTER stage, ADD COLUMN current_sheet VARCHAR(255) NULL AFTER overall_percent, ADD COLUMN current_chunk INT NOT NULL DEFAULT 0 AFTER current_sheet, ADD COLUMN total_chunks INT NOT NULL DEFAULT 0 AFTER current_chunk, ADD COLUMN sheet_total INT NOT NULL DEFAULT 0 AFTER total_chunks, ADD COLUMN sheet_processed INT NOT NULL DEFAULT 0 AFTER sheet_total, ADD COLUMN material_count INT NOT NULL DEFAULT 0 AFTER sheet_processed, ADD COLUMN food_count INT NOT NULL DEFAULT 0 AFTER material_count, ADD COLUMN impa_matched_count INT NOT NULL DEFAULT 0 AFTER food_count, ADD COLUMN category_matched_count INT NOT NULL DEFAULT 0 AFTER impa_matched_count, ADD COLUMN pending_review_count INT NOT NULL DEFAULT 0 AFTER category_matched_count, ADD COLUMN image_total INT NOT NULL DEFAULT 0 AFTER pending_review_count, ADD COLUMN image_processed INT NOT NULL DEFAULT 0 AFTER image_total, ADD COLUMN failed_count INT NOT NULL DEFAULT 0 AFTER image_processed, ADD COLUMN event_seq BIGINT NOT NULL DEFAULT 0 AFTER failed_count, ADD COLUMN source_file_sha256 VARCHAR(64) NULL AFTER source_file_name, ADD UNIQUE KEY uk_shop_import_job(job_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE shop_sku_import_preview_row MODIFY COLUMN source_sheet VARCHAR(255) NULL;

SET @has_source_item := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'shop_sku_import_preview_row' AND column_name = 'source_item_id'
);
SET @sql := IF(@has_source_item = 0,
  'ALTER TABLE shop_sku_import_preview_row ADD COLUMN source_item_id VARCHAR(320) NULL AFTER source_sheet, ADD COLUMN standard_library_type VARCHAR(30) NULL AFTER source_item_id, ADD COLUMN standard_category_code VARCHAR(50) NULL AFTER standard_library_type, ADD COLUMN impa_item_id BIGINT UNSIGNED NULL AFTER impa_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE shop_sku_import_preview_row MODIFY COLUMN impa_item_id BIGINT UNSIGNED NULL;

SET @has_preview_material_kind := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'shop_sku_import_preview_row' AND column_name = 'material_kind'
);
SET @sql := IF(@has_preview_material_kind = 0,
  'ALTER TABLE shop_sku_import_preview_row ADD COLUMN material_kind VARCHAR(30) NULL AFTER product_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_preview_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name AND table_name = 'shop_sku_import_preview_row' AND index_name = 'uk_shop_preview_batch_type_row'
);
SET @sql := IF(@has_preview_unique > 0,
  'ALTER TABLE shop_sku_import_preview_row DROP INDEX uk_shop_preview_batch_type_row',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_source_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name AND table_name = 'shop_sku_import_preview_row' AND index_name = 'uk_shop_preview_batch_source'
);
SET @sql := IF(@has_source_unique = 0,
  'ALTER TABLE shop_sku_import_preview_row ADD UNIQUE KEY uk_shop_preview_batch_source(batch_id, source_sheet, row_no)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS shop_sku_import_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  source_item_id VARCHAR(320) NOT NULL,
  source_sheet VARCHAR(255) NOT NULL,
  source_row_no INT NOT NULL,
  sku_id BIGINT NULL,
  action VARCHAR(30) NOT NULL DEFAULT 'PREVIEW',
  raw_row_hash VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shop_import_item_source(batch_id, source_item_id),
  KEY idx_shop_import_item_sku(sku_id),
  CONSTRAINT fk_shop_import_item_batch FOREIGN KEY(batch_id) REFERENCES shop_sku_import_batch(id) ON DELETE CASCADE,
  CONSTRAINT fk_shop_import_item_sku FOREIGN KEY(sku_id) REFERENCES shop_sku(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Idempotent source-row to SKU link';
