CREATE TABLE IF NOT EXISTS food_demand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  buyer_company_id BIGINT NOT NULL,
  demand_no VARCHAR(80) NOT NULL,
  inquiry_no VARCHAR(120) NULL,
  vessel_name VARCHAR(255) NOT NULL,
  supply_port VARCHAR(255) NOT NULL,
  vessel_eta DATETIME NOT NULL,
  currency VARCHAR(20) NOT NULL DEFAULT 'USD',
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  source_file_name VARCHAR(500) NULL,
  source_sheet_name VARCHAR(255) NULL,
  item_count INT NOT NULL DEFAULT 0,
  matched_count INT NOT NULL DEFAULT 0,
  pending_count INT NOT NULL DEFAULT 0,
  quote_markup_percent DECIMAL(8,4) NOT NULL DEFAULT 10,
  fixed_freight_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  fixed_customs_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  fixed_crane_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  fixed_other_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  supply_mode VARCHAR(20) NOT NULL DEFAULT 'SEA',
  fixed_provider_type VARCHAR(20) NULL,
  fixed_provider_id VARCHAR(80) NULL,
  fixed_provider_name VARCHAR(255) NULL,
  traffic_service_json JSON NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_demand_company_no (buyer_company_id, demand_no),
  KEY idx_food_demand_company_status (buyer_company_id, status, updated_at),
  UNIQUE KEY uk_food_demand_company_inquiry_no (buyer_company_id, inquiry_no),
  CONSTRAINT fk_food_demand_company FOREIGN KEY (buyer_company_id) REFERENCES company(id),
  CONSTRAINT fk_food_demand_created_by FOREIGN KEY (created_by) REFERENCES sys_user(id),
  CONSTRAINT fk_food_demand_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_inquiry_daily_sequence (
  buyer_company_id BIGINT NOT NULL,
  inquiry_date DATE NOT NULL,
  last_sequence INT NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (buyer_company_id, inquiry_date),
  CONSTRAINT fk_food_inquiry_sequence_company FOREIGN KEY (buyer_company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_demand_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  sequence_no INT NOT NULL,
  name_en VARCHAR(1000) NULL,
  name_zh VARCHAR(1000) NULL,
  normalized_name_en VARCHAR(1000) NULL,
  normalized_name_zh VARCHAR(1000) NULL,
  remark VARCHAR(2000) NULL,
  specification VARCHAR(500) NULL,
  normalized_specification VARCHAR(500) NULL,
  unit VARCHAR(80) NOT NULL,
  normalized_unit VARCHAR(80) NOT NULL,
  requested_quantity DECIMAL(18,4) NOT NULL,
  match_status VARCHAR(40) NOT NULL,
  match_reason VARCHAR(500) NULL,
  raw_row_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_demand_item_sequence (demand_id, sequence_no),
  KEY idx_food_demand_item_match (demand_id, match_status),
  KEY idx_food_item_name_unit (normalized_name_zh(120), normalized_unit),
  KEY idx_food_item_name_en_unit (normalized_name_en(120), normalized_unit),
  CONSTRAINT fk_food_demand_item_demand FOREIGN KEY (demand_id) REFERENCES food_demand(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_inquiry_supplier (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  buyer_company_id BIGINT NOT NULL,
  supplier_company_id BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'SENT',
  sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  quote_deadline_at DATETIME NULL,
  viewed_at DATETIME NULL,
  submitted_at DATETIME NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_inquiry_supplier (demand_id, supplier_company_id),
  KEY idx_food_inquiry_buyer (buyer_company_id, status, updated_at),
  KEY idx_food_inquiry_supplier (supplier_company_id, status, updated_at),
  CONSTRAINT fk_food_inquiry_demand FOREIGN KEY (demand_id) REFERENCES food_demand(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_inquiry_buyer FOREIGN KEY (buyer_company_id) REFERENCES company(id),
  CONSTRAINT fk_food_inquiry_supplier_company FOREIGN KEY (supplier_company_id) REFERENCES company(id),
  CONSTRAINT fk_food_inquiry_created_by FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_supplier_quote (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  inquiry_supplier_id BIGINT NOT NULL,
  buyer_company_id BIGINT NOT NULL,
  supplier_company_id BIGINT NOT NULL,
  quote_no VARCHAR(80) NOT NULL,
  currency VARCHAR(20) NOT NULL DEFAULT 'USD',
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  version_no INT NOT NULL DEFAULT 1,
  total_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
  cost_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
  quoted_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
  profit_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
  quote_markup_percent DECIMAL(8,4) NOT NULL DEFAULT 10,
  fixed_freight_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  fixed_customs_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  fixed_crane_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  fixed_other_fee DECIMAL(18,4) NOT NULL DEFAULT 0,
  supply_mode VARCHAR(20) NOT NULL DEFAULT 'SEA',
  fixed_provider_type VARCHAR(20) NULL,
  fixed_provider_id VARCHAR(80) NULL,
  fixed_provider_name VARCHAR(255) NULL,
  traffic_service_json JSON NULL,
  quoted_item_count INT NOT NULL DEFAULT 0,
  missing_item_count INT NOT NULL DEFAULT 0,
  quantity_difference_count INT NOT NULL DEFAULT 0,
  submitted_at DATETIME NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_quote_supplier_demand (demand_id, supplier_company_id),
  UNIQUE KEY uk_food_quote_no (quote_no),
  KEY idx_food_quote_buyer (buyer_company_id, status, updated_at),
  KEY idx_food_quote_supplier (supplier_company_id, status, updated_at),
  CONSTRAINT fk_food_quote_demand FOREIGN KEY (demand_id) REFERENCES food_demand(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_quote_inquiry FOREIGN KEY (inquiry_supplier_id) REFERENCES food_inquiry_supplier(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_quote_buyer FOREIGN KEY (buyer_company_id) REFERENCES company(id),
  CONSTRAINT fk_food_quote_supplier FOREIGN KEY (supplier_company_id) REFERENCES company(id),
  CONSTRAINT fk_food_quote_created_by FOREIGN KEY (created_by) REFERENCES sys_user(id),
  CONSTRAINT fk_food_quote_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_supplier_quote_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quote_id BIGINT NOT NULL,
  demand_item_id BIGINT NOT NULL,
  requested_quantity DECIMAL(18,4) NOT NULL,
  quoted_quantity DECIMAL(18,4) NULL,
  unit_price DECIMAL(18,4) NULL,
  amount DECIMAL(18,4) NULL,
  availability VARCHAR(40) NOT NULL DEFAULT 'AVAILABLE',
  price_source VARCHAR(40) NULL,
  match_status VARCHAR(40) NOT NULL DEFAULT 'MATCHED',
  supplier_remark VARCHAR(2000) NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_quote_demand_item (quote_id, demand_item_id),
  KEY idx_food_quote_item_state (quote_id, availability, price_source),
  CONSTRAINT fk_food_quote_item_quote FOREIGN KEY (quote_id) REFERENCES food_supplier_quote(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_quote_item_demand_item FOREIGN KEY (demand_item_id) REFERENCES food_demand_item(id),
  CONSTRAINT fk_food_quote_item_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_quote_import_batch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quote_id BIGINT NOT NULL,
  file_name VARCHAR(500) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'PREVIEW',
  preview_json JSON NOT NULL,
  matched_count INT NOT NULL DEFAULT 0,
  issue_count INT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  committed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_food_quote_import (quote_id, status, created_at),
  CONSTRAINT fk_food_quote_import_quote FOREIGN KEY (quote_id) REFERENCES food_supplier_quote(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_quote_import_created_by FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_purchase_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  buyer_company_id BIGINT NOT NULL,
  order_no VARCHAR(80) NOT NULL,
  strategy_type VARCHAR(40) NOT NULL,
  currency VARCHAR(20) NOT NULL DEFAULT 'USD',
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
  total_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_purchase_order_no (order_no),
  KEY idx_food_order_buyer (buyer_company_id, status, updated_at),
  KEY idx_food_order_demand (demand_id),
  CONSTRAINT fk_food_order_demand FOREIGN KEY (demand_id) REFERENCES food_demand(id),
  CONSTRAINT fk_food_order_buyer FOREIGN KEY (buyer_company_id) REFERENCES company(id),
  CONSTRAINT fk_food_order_created_by FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_purchase_order_supplier (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  supplier_company_id BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
  subtotal_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
  expected_ready_at DATETIME NULL,
  reject_reason VARCHAR(1000) NULL,
  shipment_remark VARCHAR(2000) NULL,
  confirmed_at DATETIME NULL,
  ready_at DATETIME NULL,
  shipped_at DATETIME NULL,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_order_supplier (order_id, supplier_company_id),
  KEY idx_food_supplier_order (supplier_company_id, status, updated_at),
  CONSTRAINT fk_food_order_supplier_order FOREIGN KEY (order_id) REFERENCES food_purchase_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_order_supplier_company FOREIGN KEY (supplier_company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_purchase_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  supplier_order_id BIGINT NOT NULL,
  demand_item_id BIGINT NOT NULL,
  quote_item_id BIGINT NOT NULL,
  name_en VARCHAR(1000) NULL,
  name_zh VARCHAR(1000) NULL,
  specification VARCHAR(500) NULL,
  unit VARCHAR(80) NOT NULL,
  requested_quantity DECIMAL(18,4) NOT NULL,
  ordered_quantity DECIMAL(18,4) NOT NULL,
  unit_price DECIMAL(18,4) NOT NULL,
  amount DECIMAL(18,4) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_food_order_item_order (order_id, supplier_order_id),
  KEY idx_food_order_item_demand (demand_item_id),
  CONSTRAINT fk_food_order_item_order FOREIGN KEY (order_id) REFERENCES food_purchase_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_order_item_supplier_order FOREIGN KEY (supplier_order_id) REFERENCES food_purchase_order_supplier(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_order_item_demand_item FOREIGN KEY (demand_item_id) REFERENCES food_demand_item(id),
  CONSTRAINT fk_food_order_item_quote_item FOREIGN KEY (quote_item_id) REFERENCES food_supplier_quote_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_purchase_order_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  supplier_order_id BIGINT NULL,
  event_type VARCHAR(80) NOT NULL,
  from_status VARCHAR(40) NULL,
  to_status VARCHAR(40) NOT NULL,
  remark VARCHAR(2000) NULL,
  operator_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_food_order_event (order_id, created_at),
  CONSTRAINT fk_food_order_event_order FOREIGN KEY (order_id) REFERENCES food_purchase_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_order_event_supplier FOREIGN KEY (supplier_order_id) REFERENCES food_purchase_order_supplier(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_order_event_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_settlement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  supplier_order_id BIGINT NOT NULL,
  buyer_company_id BIGINT NOT NULL,
  supplier_company_id BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING_INVOICE',
  amount DECIMAL(18,4) NOT NULL,
  invoice_no VARCHAR(160) NULL,
  invoice_file_id VARCHAR(120) NULL,
  settled_at DATETIME NULL,
  paid_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_settlement_supplier_order (supplier_order_id),
  KEY idx_food_settlement_buyer (buyer_company_id, status, updated_at),
  KEY idx_food_settlement_supplier (supplier_company_id, status, updated_at),
  CONSTRAINT fk_food_settlement_order FOREIGN KEY (order_id) REFERENCES food_purchase_order(id),
  CONSTRAINT fk_food_settlement_supplier_order FOREIGN KEY (supplier_order_id) REFERENCES food_purchase_order_supplier(id),
  CONSTRAINT fk_food_settlement_buyer FOREIGN KEY (buyer_company_id) REFERENCES company(id),
  CONSTRAINT fk_food_settlement_supplier FOREIGN KEY (supplier_company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS food_evaluation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  buyer_company_id BIGINT NOT NULL,
  supplier_company_id BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING_EVALUATION',
  quality_rating INT NULL,
  logistics_rating INT NULL,
  comment VARCHAR(2000) NULL,
  review_remark VARCHAR(2000) NULL,
  submitted_at DATETIME NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_evaluation_order_supplier (order_id, supplier_company_id),
  KEY idx_food_evaluation_buyer (buyer_company_id, status, updated_at),
  KEY idx_food_evaluation_supplier (supplier_company_id, status, updated_at),
  CONSTRAINT fk_food_evaluation_order FOREIGN KEY (order_id) REFERENCES food_purchase_order(id),
  CONSTRAINT fk_food_evaluation_buyer FOREIGN KEY (buyer_company_id) REFERENCES company(id),
  CONSTRAINT fk_food_evaluation_supplier FOREIGN KEY (supplier_company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('PROCUREMENT_FOOD_VIEW', '伙食采购入口查看', 'MENU', 'PROCUREMENT_FOOD', 'VIEW', 1),
  ('FOOD_INQUIRY_VIEW', '伙食询价管理查看', 'MENU', 'FOOD_INQUIRIES', 'VIEW', 1),
  ('FOOD_QUOTE_VIEW', '伙食报价管理查看', 'MENU', 'FOOD_QUOTES', 'VIEW', 1),
  ('FOOD_COMPARISON_VIEW', '伙食比价管理查看', 'MENU', 'FOOD_COMPARISON', 'VIEW', 1),
  ('FOOD_ORDER_VIEW', '伙食采购管理查看', 'MENU', 'FOOD_ORDERS', 'VIEW', 1),
  ('FOOD_SUPPLIER_ORDER_VIEW', '伙食供货订单查看', 'MENU', 'FOOD_SUPPLIER_ORDERS', 'VIEW', 1),
  ('FOOD_SETTLEMENT_VIEW', '伙食采购结算查看', 'MENU', 'FOOD_SETTLEMENTS', 'VIEW', 1),
  ('FOOD_SUPPLIER_SETTLEMENT_VIEW', '伙食供应结算查看', 'MENU', 'FOOD_SUPPLIER_SETTLEMENTS', 'VIEW', 1),
  ('FOOD_EVALUATION_VIEW', '伙食供应评价查看', 'MENU', 'FOOD_EVALUATIONS', 'VIEW', 1)
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
VALUES
  ('FOOD_PROCUREMENT_GROUP', '伙食采购管理', '', 'Utensils', NULL, 20, NULL, 'SHIP_AGENT,SUPPLIER,PLATFORM_ADMIN', 1),
  ('PROCUREMENT_FOOD', '伙食采购入口', '/procurement/food', 'Utensils', 'FOOD_PROCUREMENT_GROUP', 0, 'PROCUREMENT_FOOD_VIEW', 'SHIP_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_INQUIRIES', '询价管理', '/food/inquiries', 'FileSearch', 'FOOD_PROCUREMENT_GROUP', 10, 'FOOD_INQUIRY_VIEW', 'SHIP_AGENT,SUPPLIER,PLATFORM_ADMIN', 1),
  ('FOOD_QUOTES', '报价管理', '/food/quotes', 'FileText', 'FOOD_PROCUREMENT_GROUP', 20, 'FOOD_QUOTE_VIEW', 'SHIP_AGENT,SUPPLIER,PLATFORM_ADMIN', 1),
  ('FOOD_COMPARISON', '比价管理', '/food/comparison', 'Scale', 'FOOD_PROCUREMENT_GROUP', 30, 'FOOD_COMPARISON_VIEW', 'SHIP_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_ORDERS', '采购管理', '/food/orders', 'ShoppingCart', 'FOOD_PROCUREMENT_GROUP', 40, 'FOOD_ORDER_VIEW', 'SHIP_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_SUPPLIER_ORDERS', '供采管理', '/supplier/food/orders', 'ShoppingCart', 'FOOD_PROCUREMENT_GROUP', 50, 'FOOD_SUPPLIER_ORDER_VIEW', 'SUPPLIER,PLATFORM_ADMIN', 1),
  ('FOOD_SETTLEMENTS', '结算管理', '/food/settlements', 'Receipt', 'FOOD_PROCUREMENT_GROUP', 60, 'FOOD_SETTLEMENT_VIEW', 'SHIP_AGENT,PLATFORM_ADMIN', 1),
  ('FOOD_SUPPLIER_SETTLEMENTS', '供应结算', '/supplier/food/settlements', 'Receipt', 'FOOD_PROCUREMENT_GROUP', 70, 'FOOD_SUPPLIER_SETTLEMENT_VIEW', 'SUPPLIER,PLATFORM_ADMIN', 1),
  ('FOOD_EVALUATIONS', '评价体系', '/food/evaluations', 'Star', 'FOOD_PROCUREMENT_GROUP', 80, 'FOOD_EVALUATION_VIEW', 'SHIP_AGENT,PLATFORM_ADMIN', 1)
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
  ON permission.permission_code IN (
    'PROCUREMENT_FOOD_VIEW', 'FOOD_INQUIRY_VIEW', 'FOOD_QUOTE_VIEW', 'FOOD_COMPARISON_VIEW',
    'FOOD_ORDER_VIEW', 'FOOD_SUPPLIER_ORDER_VIEW', 'FOOD_SETTLEMENT_VIEW',
    'FOOD_SUPPLIER_SETTLEMENT_VIEW', 'FOOD_EVALUATION_VIEW'
  )
WHERE role.enabled = 1
  AND permission.enabled = 1
  AND (
    role.role_code = 'PLATFORM_ADMIN'
    OR (role.role_code = 'SHIP_AGENT' AND permission.permission_code NOT IN ('FOOD_SUPPLIER_ORDER_VIEW', 'FOOD_SUPPLIER_SETTLEMENT_VIEW'))
    OR (role.role_code = 'SUPPLIER' AND permission.permission_code IN ('FOOD_INQUIRY_VIEW', 'FOOD_QUOTE_VIEW', 'FOOD_SUPPLIER_ORDER_VIEW', 'FOOD_SUPPLIER_SETTLEMENT_VIEW'))
    OR (role.company_id IS NOT NULL AND (role.role_type = 'COMPANY_ADMIN' OR role.role_code = CONCAT('COMPANY_ADMIN_', role.company_id)))
  );
