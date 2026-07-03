-- Material quote template retention and actual quote export fields.
-- Idempotent migration. Scope: material demand import/compare/export only.

SET @schema_name := DATABASE();

SET @has_source_file_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'source_file_id'
);
SET @sql := IF(@has_source_file_id = 0,
  'ALTER TABLE material_demand ADD COLUMN source_file_id VARCHAR(80) NULL COMMENT ''Original uploaded template file id'' AFTER source_file_name',
  'SELECT ''material_demand.source_file_id already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_actual_quote_price := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand_item' AND column_name = 'actual_quote_price'
);
SET @sql := IF(@has_actual_quote_price = 0,
  'ALTER TABLE material_demand_item ADD COLUMN actual_quote_price DECIMAL(18,4) NULL COMMENT ''Ship agent actual quotation price'' AFTER candidates_json',
  'SELECT ''material_demand_item.actual_quote_price already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_actual_quote_currency := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand_item' AND column_name = 'actual_quote_currency'
);
SET @sql := IF(@has_actual_quote_currency = 0,
  'ALTER TABLE material_demand_item ADD COLUMN actual_quote_currency VARCHAR(20) NULL COMMENT ''Actual quotation currency'' AFTER actual_quote_price',
  'SELECT ''material_demand_item.actual_quote_currency already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_quote_markup_percent := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand_item' AND column_name = 'quote_markup_percent'
);
SET @sql := IF(@has_quote_markup_percent = 0,
  'ALTER TABLE material_demand_item ADD COLUMN quote_markup_percent DECIMAL(10,4) NULL COMMENT ''Global or row quote markup percent'' AFTER actual_quote_currency',
  'SELECT ''material_demand_item.quote_markup_percent already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_quote_supplier_sku_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand_item' AND column_name = 'quote_supplier_sku_id'
);
SET @sql := IF(@has_quote_supplier_sku_id = 0,
  'ALTER TABLE material_demand_item ADD COLUMN quote_supplier_sku_id BIGINT NULL COMMENT ''Selected supplier SKU for saved quote'' AFTER quote_markup_percent',
  'SELECT ''material_demand_item.quote_supplier_sku_id already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_quote_selected_unit := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand_item' AND column_name = 'quote_selected_unit'
);
SET @sql := IF(@has_quote_selected_unit = 0,
  'ALTER TABLE material_demand_item ADD COLUMN quote_selected_unit VARCHAR(80) NULL COMMENT ''Selected comparison unit for saved quote'' AFTER quote_supplier_sku_id',
  'SELECT ''material_demand_item.quote_selected_unit already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_quote_unit_price := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand_item' AND column_name = 'quote_unit_price'
);
SET @sql := IF(@has_quote_unit_price = 0,
  'ALTER TABLE material_demand_item ADD COLUMN quote_unit_price DECIMAL(18,4) NULL COMMENT ''Saved supplier CNY unit price'' AFTER quote_selected_unit',
  'SELECT ''material_demand_item.quote_unit_price already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_quote_unit_price_usd := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand_item' AND column_name = 'quote_unit_price_usd'
);
SET @sql := IF(@has_quote_unit_price_usd = 0,
  'ALTER TABLE material_demand_item ADD COLUMN quote_unit_price_usd DECIMAL(18,4) NULL COMMENT ''Saved supplier USD unit price'' AFTER quote_unit_price',
  'SELECT ''material_demand_item.quote_unit_price_usd already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_quote_strategy_type := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'material_demand_item' AND column_name = 'quote_strategy_type'
);
SET @sql := IF(@has_quote_strategy_type = 0,
  'ALTER TABLE material_demand_item ADD COLUMN quote_strategy_type VARCHAR(40) NULL COMMENT ''Strategy used when quote was saved'' AFTER quote_unit_price_usd',
  'SELECT ''material_demand_item.quote_strategy_type already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_source_file_idx := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND index_name = 'idx_material_demand_source_file_id'
);
SET @sql := IF(@has_source_file_idx = 0,
  'CREATE INDEX idx_material_demand_source_file_id ON material_demand (source_file_id)',
  'SELECT ''idx_material_demand_source_file_id already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
