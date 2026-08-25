-- 伙食比价同步产品策略设置，仅使用伙食商品名称、规格和商家标签。
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_demand' AND column_name = 'comparison_mixed_supplier_count');
SET @sql := IF(@has_column = 0, 'ALTER TABLE food_demand ADD COLUMN comparison_mixed_supplier_count INT NOT NULL DEFAULT 3 AFTER comparison_selected_demand_item_ids_json', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_demand' AND column_name = 'comparison_price_enabled');
SET @sql := IF(@has_column = 0, 'ALTER TABLE food_demand ADD COLUMN comparison_price_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER comparison_mixed_supplier_count', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_demand' AND column_name = 'comparison_price_level');
SET @sql := IF(@has_column = 0, 'ALTER TABLE food_demand ADD COLUMN comparison_price_level TINYINT NOT NULL DEFAULT 5 AFTER comparison_price_enabled', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_demand' AND column_name = 'comparison_quality_enabled');
SET @sql := IF(@has_column = 0, 'ALTER TABLE food_demand ADD COLUMN comparison_quality_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER comparison_price_level', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_demand' AND column_name = 'comparison_quality_level');
SET @sql := IF(@has_column = 0, 'ALTER TABLE food_demand ADD COLUMN comparison_quality_level TINYINT NOT NULL DEFAULT 3 AFTER comparison_quality_enabled', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_demand' AND column_name = 'comparison_core_demand_item_ids_json');
SET @sql := IF(@has_column = 0, 'ALTER TABLE food_demand ADD COLUMN comparison_core_demand_item_ids_json JSON NULL AFTER comparison_quality_level', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE food_demand
SET comparison_mixed_supplier_count = 3
WHERE comparison_mixed_supplier_count IS NULL OR comparison_mixed_supplier_count < 2;
