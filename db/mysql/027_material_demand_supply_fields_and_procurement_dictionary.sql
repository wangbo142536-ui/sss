-- Material demand supply fields and procurement dictionary seeds.
-- Idempotent: only adds missing columns/indexes, updates dictionary seeds, and hides the old material quote entry.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @has_application_nullable := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'material_demand'
    AND COLUMN_NAME = 'application_no'
    AND IS_NULLABLE = 'YES'
);
SET @alter_application_nullable_sql := IF(
  @has_application_nullable = 0,
  'ALTER TABLE material_demand MODIFY COLUMN application_no VARCHAR(80) NULL COMMENT ''Application number''',
  'SELECT 1'
);
PREPARE alter_application_nullable_stmt FROM @alter_application_nullable_sql;
EXECUTE alter_application_nullable_stmt;
DEALLOCATE PREPARE alter_application_nullable_stmt;

SET @has_supply_port_code := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'material_demand'
    AND COLUMN_NAME = 'supply_port_code'
);
SET @add_supply_port_code_sql := IF(
  @has_supply_port_code = 0,
  'ALTER TABLE material_demand ADD COLUMN supply_port_code VARCHAR(80) NULL COMMENT ''Supply port dictionary code'' AFTER vessel_name',
  'SELECT 1'
);
PREPARE add_supply_port_code_stmt FROM @add_supply_port_code_sql;
EXECUTE add_supply_port_code_stmt;
DEALLOCATE PREPARE add_supply_port_code_stmt;

SET @has_supply_port_name := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'material_demand'
    AND COLUMN_NAME = 'supply_port_name'
);
SET @add_supply_port_name_sql := IF(
  @has_supply_port_name = 0,
  'ALTER TABLE material_demand ADD COLUMN supply_port_name VARCHAR(160) NULL COMMENT ''Supply port display name'' AFTER supply_port_code',
  'SELECT 1'
);
PREPARE add_supply_port_name_stmt FROM @add_supply_port_name_sql;
EXECUTE add_supply_port_name_stmt;
DEALLOCATE PREPARE add_supply_port_name_stmt;

SET @has_vessel_eta := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'material_demand'
    AND COLUMN_NAME = 'vessel_eta'
);
SET @add_vessel_eta_sql := IF(
  @has_vessel_eta = 0,
  'ALTER TABLE material_demand ADD COLUMN vessel_eta VARCHAR(40) NULL COMMENT ''Estimated vessel arrival time'' AFTER supply_port_name',
  'SELECT 1'
);
PREPARE add_vessel_eta_stmt FROM @add_vessel_eta_sql;
EXECUTE add_vessel_eta_stmt;
DEALLOCATE PREPARE add_vessel_eta_stmt;

SET @has_supply_port_index := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'material_demand'
    AND INDEX_NAME = 'idx_material_demand_supply_port'
);
SET @add_supply_port_index_sql := IF(
  @has_supply_port_index = 0,
  'CREATE INDEX idx_material_demand_supply_port ON material_demand (company_id, supply_port_code)',
  'SELECT 1'
);
PREPARE add_supply_port_index_stmt FROM @add_supply_port_index_sql;
EXECUTE add_supply_port_index_stmt;
DEALLOCATE PREPARE add_supply_port_index_stmt;

INSERT INTO sys_dictionary_type (type_code, type_name, description, sort_order, enabled)
VALUES
  ('PORT', '港口', '', 10, 1),
  ('UNIT', '单位', '物料、采购、报价常用单位', 30, 1),
  ('CURRENCY', '币种', '报价与订单币种', 40, 1)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);

INSERT INTO sys_dictionary_item (type_code, item_code, item_name, item_value, item_name_en, sort_order, enabled, built_in)
VALUES
  ('PORT', 'ZHOUSHAN', '舟山港', 'ZHOUSHAN', 'Zhoushan Port', 10, 1, 1),
  ('PORT', 'NINGBO', '宁波港', 'NINGBO', 'Ningbo Port', 20, 1, 1),
  ('PORT', 'SHANGHAI', '上海港', 'SHANGHAI', 'Shanghai Port', 30, 1, 1),
  ('PORT', 'TAICANG', '太仓港', 'TAICANG', 'Taicang Port', 40, 1, 1),
  ('PORT', 'NANTONG', '南通港', 'NANTONG', 'Nantong Port', 50, 1, 1),
  ('PORT', 'QINGDAO', '青岛港', 'QINGDAO', 'Qingdao Port', 60, 1, 1),
  ('PORT', 'TIANJIN', '天津港', 'TIANJIN', 'Tianjin Port', 70, 1, 1),
  ('PORT', 'GUANGZHOU', '广州港', 'GUANGZHOU', 'Guangzhou Port', 80, 1, 1),
  ('PORT', 'SHENZHEN', '深圳港', 'SHENZHEN', 'Shenzhen Port', 90, 1, 1),
  ('PORT', 'XIAMEN', '厦门港', 'XIAMEN', 'Xiamen Port', 100, 1, 1),
  ('PORT', 'DALIAN', '大连港', 'DALIAN', 'Dalian Port', 110, 1, 1),
  ('PORT', 'HONG_KONG', '香港港', 'HONG_KONG', 'Hong Kong Port', 120, 1, 1),
  ('PORT', 'KAOHSIUNG', '高雄港', 'KAOHSIUNG', 'Kaohsiung Port', 130, 1, 1),
  ('PORT', 'SINGAPORE', '新加坡港', 'SINGAPORE', 'Singapore Port', 200, 1, 1),
  ('PORT', 'ROTTERDAM', '鹿特丹港', 'ROTTERDAM', 'Rotterdam Port', 210, 1, 1),
  ('PORT', 'BUSAN', '釜山港', 'BUSAN', 'Busan Port', 220, 1, 1),
  ('PORT', 'ANTWERP', '安特卫普港', 'ANTWERP', 'Antwerp Port', 230, 1, 1),
  ('PORT', 'HAMBURG', '汉堡港', 'HAMBURG', 'Hamburg Port', 240, 1, 1),
  ('PORT', 'LOS_ANGELES', '洛杉矶港', 'LOS_ANGELES', 'Los Angeles Port', 250, 1, 1),
  ('PORT', 'LONG_BEACH', '长滩港', 'LONG_BEACH', 'Long Beach Port', 260, 1, 1),
  ('PORT', 'JEBEL_ALI', '杰贝阿里港', 'JEBEL_ALI', 'Jebel Ali Port', 270, 1, 1),
  ('PORT', 'MANILA', '马尼拉港', 'MANILA', 'Manila Port', 280, 1, 1),
  ('PORT', 'TOKYO', '东京港', 'TOKYO', 'Tokyo Port', 290, 1, 1),
  ('PORT', 'YOKOHAMA', '横滨港', 'YOKOHAMA', 'Yokohama Port', 300, 1, 1),
  ('PORT', 'KOBE', '神户港', 'KOBE', 'Kobe Port', 310, 1, 1),
  ('PORT', 'INCHEON', '仁川港', 'INCHEON', 'Incheon Port', 320, 1, 1),
  ('UNIT', 'PCS', '个', 'PCS', 'Piece', 10, 1, 1),
  ('UNIT', 'PIECE', '件', 'PIECE', 'Piece', 20, 1, 1),
  ('UNIT', 'SET', '套', 'SET', 'Set', 30, 1, 1),
  ('UNIT', 'BOX', '箱', 'BOX', 'Box', 40, 1, 1),
  ('UNIT', 'PACKAGE', '包', 'PACKAGE', 'Package', 50, 1, 1),
  ('UNIT', 'ROLL', '卷', 'ROLL', 'Roll', 60, 1, 1),
  ('UNIT', 'M', '米', 'M', 'Meter', 70, 1, 1),
  ('UNIT', 'KG', '公斤', 'KG', 'Kilogram', 80, 1, 1),
  ('UNIT', 'L', '升', 'L', 'Liter', 90, 1, 1),
  ('UNIT', 'BARREL', '桶', 'BARREL', 'Barrel', 100, 1, 1),
  ('UNIT', 'BOTTLE', '瓶', 'BOTTLE', 'Bottle', 110, 1, 1),
  ('UNIT', 'CARTON', '盒', 'CARTON', 'Carton', 120, 1, 1),
  ('UNIT', 'BAG', '袋', 'BAG', 'Bag', 130, 1, 1),
  ('UNIT', 'DOZEN', '打', 'DOZEN', 'Dozen', 140, 1, 1),
  ('UNIT', 'PAIR', '双', 'PAIR', 'Pair', 150, 1, 1),
  ('UNIT', 'UNIT', '台', 'UNIT', 'Unit', 160, 1, 1),
  ('UNIT', 'EA', '只', 'EA', 'Each', 170, 1, 1),
  ('UNIT', 'ROOT', '根', 'ROOT', 'Root', 180, 1, 1),
  ('UNIT', 'SHEET', '张', 'SHEET', 'Sheet', 190, 1, 1),
  ('UNIT', 'TABLET', '片', 'TABLET', 'Tablet', 200, 1, 1),
  ('UNIT', 'GROUP', '组', 'GROUP', 'Group', 210, 1, 1),
  ('UNIT', 'CAN', '罐', 'CAN', 'Can', 220, 1, 1),
  ('CURRENCY', 'CNY', '人民币', 'CNY', 'CNY', 10, 1, 1),
  ('CURRENCY', 'USD', '美元', 'USD', 'USD', 20, 1, 1),
  ('CURRENCY', 'EUR', '欧元', 'EUR', 'EUR', 30, 1, 1),
  ('CURRENCY', 'HKD', '港币', 'HKD', 'HKD', 40, 1, 1)
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  item_value = VALUES(item_value),
  item_name_en = VALUES(item_name_en),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled),
  built_in = VALUES(built_in);

UPDATE sys_menu
SET enabled = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'QUOTES'
   OR route_path = '/quotes';
