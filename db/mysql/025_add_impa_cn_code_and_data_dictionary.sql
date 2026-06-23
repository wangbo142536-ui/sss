-- IMPA CN code and shared data dictionary.
-- Idempotent: creates missing structures and seeds common dictionary data without deleting business data.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @has_impa_cn_code := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'impa_item'
    AND COLUMN_NAME = 'cn_code'
);
SET @add_impa_cn_code_sql := IF(
  @has_impa_cn_code = 0,
  'ALTER TABLE impa_item ADD COLUMN cn_code VARCHAR(80) NULL COMMENT ''CN编码'' AFTER impa_code',
  'SELECT 1'
);
PREPARE add_impa_cn_code_stmt FROM @add_impa_cn_code_sql;
EXECUTE add_impa_cn_code_stmt;
DEALLOCATE PREPARE add_impa_cn_code_stmt;

SET @has_impa_cn_code_index := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'impa_item'
    AND INDEX_NAME = 'idx_impa_item_cn_code'
);
SET @add_impa_cn_code_index_sql := IF(
  @has_impa_cn_code_index = 0,
  'CREATE INDEX idx_impa_item_cn_code ON impa_item (cn_code)',
  'SELECT 1'
);
PREPARE add_impa_cn_code_index_stmt FROM @add_impa_cn_code_index_sql;
EXECUTE add_impa_cn_code_index_stmt;
DEALLOCATE PREPARE add_impa_cn_code_index_stmt;

CREATE TABLE IF NOT EXISTS sys_dictionary_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(80) NOT NULL,
  type_name VARCHAR(120) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_dictionary_type_code (type_code),
  KEY idx_sys_dictionary_type_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典类型';

CREATE TABLE IF NOT EXISTS sys_dictionary_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(80) NOT NULL,
  item_code VARCHAR(120) NOT NULL,
  item_name VARCHAR(160) NOT NULL,
  item_value VARCHAR(200) NULL,
  item_name_en VARCHAR(160) NULL,
  description VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  built_in TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_dictionary_item_code (type_code, item_code),
  KEY idx_sys_dictionary_item_type_sort (type_code, enabled, sort_order),
  CONSTRAINT fk_sys_dictionary_item_type
    FOREIGN KEY (type_code) REFERENCES sys_dictionary_type(type_code)
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典明细';

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('DATA_DICTIONARY_MANAGE', '数据字典管理', 'MENU', 'DATA_DICTIONARY', 'MANAGE', 1)
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_type = VALUES(permission_type),
  resource_code = VALUES(resource_code),
  action_code = VALUES(action_code),
  enabled = VALUES(enabled);

INSERT INTO sys_menu (
  menu_code, menu_name, route_path, icon, parent_code, sort_order,
  required_permission, visible_roles, enabled
)
VALUES (
  'DATA_DICTIONARY', '数据字典', '/admin/dictionaries', 'BookOpen', 'BASIC_MANAGEMENT', 50,
  'DATA_DICTIONARY_MANAGE', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1
)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  route_path = VALUES(route_path),
  icon = VALUES(icon),
  parent_code = VALUES(parent_code),
  sort_order = VALUES(sort_order),
  required_permission = VALUES(required_permission),
  visible_roles = VALUES(visible_roles),
  enabled = VALUES(enabled);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'DATA_DICTIONARY_MANAGE'
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NOT NULL
  AND (
    role.role_type = 'COMPANY_ADMIN'
    OR role.role_code = CONCAT('COMPANY_ADMIN_', role.company_id)
  );

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'DATA_DICTIONARY_MANAGE'
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NULL
  AND role.role_code = 'PLATFORM_ADMIN';

INSERT INTO sys_dictionary_type (type_code, type_name, description, sort_order, enabled)
VALUES
  ('PORT', '港口', '常用补给港口、到港港口', 10, 1),
  ('TERMINAL', '码头', '港区、码头、锚地等位置', 20, 1),
  ('UNIT', '单位', '物料、采购、报价常用单位', 30, 1),
  ('CURRENCY', '币种', '报价与订单币种', 40, 1),
  ('MATERIAL_DEMAND_STATUS', '询价单状态', '物料需求/询价单状态', 50, 1),
  ('PURCHASE_ORDER_STATUS', '采购单状态', '采购主单状态', 60, 1),
  ('SUPPLIER_ORDER_STATUS', '供货商确认单状态', '供货商子单确认和备货状态', 70, 1),
  ('SHELF_STATUS', '上下架状态', '供货商 SKU 上下架状态', 80, 1),
  ('PRODUCT_TYPE', '商品类型', '物料、伙食等商品类型', 90, 1),
  ('PACKAGING_METHOD', '包装方式', '下单与供货商确认包装方式', 100, 1),
  ('DISCOUNT_TYPE', '折扣方式', '供货商确认时可选折扣方式', 110, 1),
  ('MATCH_RESULT', '匹配结果', '物料导入与比价匹配结果', 120, 1)
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
  ('TERMINAL', 'ZHOUSHAN_ANCHORAGE', '舟山锚地', 'ZHOUSHAN_ANCHORAGE', 'Zhoushan Anchorage', 10, 1, 1),
  ('TERMINAL', 'ZHOUSHAN_PORT_AREA', '舟山港区', 'ZHOUSHAN_PORT_AREA', 'Zhoushan Port Area', 20, 1, 1),
  ('TERMINAL', 'NINGBO_BEILUN', '宁波北仑码头', 'NINGBO_BEILUN', 'Ningbo Beilun Terminal', 30, 1, 1),
  ('UNIT', 'PCS', '个', 'PCS', 'Piece', 10, 1, 1),
  ('UNIT', 'SET', '套', 'SET', 'Set', 20, 1, 1),
  ('UNIT', 'BOX', '箱', 'BOX', 'Box', 30, 1, 1),
  ('UNIT', 'CARTON', '纸箱', 'CARTON', 'Carton', 40, 1, 1),
  ('UNIT', 'KG', '千克', 'KG', 'Kilogram', 50, 1, 1),
  ('UNIT', 'M', '米', 'M', 'Meter', 60, 1, 1),
  ('UNIT', 'L', '升', 'L', 'Liter', 70, 1, 1),
  ('CURRENCY', 'CNY', '人民币', 'CNY', 'CNY', 10, 1, 1),
  ('CURRENCY', 'USD', '美元', 'USD', 'USD', 20, 1, 1),
  ('CURRENCY', 'EUR', '欧元', 'EUR', 'EUR', 30, 1, 1),
  ('MATERIAL_DEMAND_STATUS', 'SAVED', '已保存', 'SAVED', 'Saved', 10, 1, 1),
  ('MATERIAL_DEMAND_STATUS', 'COMPARING', '比价中', 'COMPARING', 'Comparing', 20, 1, 1),
  ('MATERIAL_DEMAND_STATUS', 'ORDERED', '已下单', 'ORDERED', 'Ordered', 30, 1, 1),
  ('MATERIAL_DEMAND_STATUS', 'DISCARDED', '废弃', 'DISCARDED', 'Discarded', 40, 1, 1),
  ('PURCHASE_ORDER_STATUS', 'PENDING_SUPPLIER_CONFIRM', '待确认', 'PENDING_SUPPLIER_CONFIRM', 'Pending supplier confirmation', 10, 1, 1),
  ('PURCHASE_ORDER_STATUS', 'PARTIALLY_CONFIRMED', '部分确认', 'PARTIALLY_CONFIRMED', 'Partially confirmed', 20, 1, 1),
  ('PURCHASE_ORDER_STATUS', 'PREPARING', '备货中', 'PREPARING', 'Preparing', 30, 1, 1),
  ('PURCHASE_ORDER_STATUS', 'REJECTED', '已拒绝', 'REJECTED', 'Rejected', 40, 1, 1),
  ('PURCHASE_ORDER_STATUS', 'CANCELED', '已取消', 'CANCELED', 'Canceled', 50, 1, 1),
  ('PURCHASE_ORDER_STATUS', 'DISCARDED', '废弃', 'DISCARDED', 'Discarded', 60, 1, 1),
  ('SUPPLIER_ORDER_STATUS', 'PENDING_SUPPLIER_CONFIRM', '待确认', 'PENDING_SUPPLIER_CONFIRM', 'Pending confirmation', 10, 1, 1),
  ('SUPPLIER_ORDER_STATUS', 'PREPARING', '备货中', 'PREPARING', 'Preparing', 20, 1, 1),
  ('SUPPLIER_ORDER_STATUS', 'REJECTED', '已拒绝', 'REJECTED', 'Rejected', 30, 1, 1),
  ('SUPPLIER_ORDER_STATUS', 'CANCELED', '已取消', 'CANCELED', 'Canceled', 40, 1, 1),
  ('SUPPLIER_ORDER_STATUS', 'DISCARDED', '废弃', 'DISCARDED', 'Discarded', 50, 1, 1),
  ('SHELF_STATUS', 'ON_SHELF', '上架', 'ON_SHELF', 'On shelf', 10, 1, 1),
  ('SHELF_STATUS', 'OFF_SHELF', '下架', 'OFF_SHELF', 'Off shelf', 20, 1, 1),
  ('PRODUCT_TYPE', 'MATERIAL', '物料', 'MATERIAL', 'Material', 10, 1, 1),
  ('PRODUCT_TYPE', 'FOOD', '伙食', 'FOOD', 'Food', 20, 1, 1),
  ('PACKAGING_METHOD', 'UNIFIED_PACKAGING', '常规包装', 'UNIFIED_PACKAGING', 'Unified packaging', 10, 1, 1),
  ('PACKAGING_METHOD', 'SUPPLIER_PACKAGING', '供货商自行包装', 'SUPPLIER_PACKAGING', 'Supplier packaging', 20, 1, 1),
  ('DISCOUNT_TYPE', 'AMOUNT', '按金额', 'AMOUNT', 'Amount', 10, 1, 1),
  ('DISCOUNT_TYPE', 'PERCENT', '按比例', 'PERCENT', 'Percent', 20, 1, 1),
  ('MATCH_RESULT', 'EXACT', '精准匹配', 'EXACT', 'Exact', 10, 1, 1),
  ('MATCH_RESULT', 'SIMILAR', '相似匹配', 'SIMILAR', 'Similar', 20, 1, 1),
  ('MATCH_RESULT', 'UNMATCHED', '未匹配', 'UNMATCHED', 'Unmatched', 30, 1, 1)
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  item_value = VALUES(item_value),
  item_name_en = VALUES(item_name_en),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled),
  built_in = VALUES(built_in);
