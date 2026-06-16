-- Material procurement purchase order tables.
-- Idempotent migration: creates purchase order master, supplier sub-order, items, and events.
-- Scope: material procurement only. Does not modify demand, SKU, permission, or existing business data.

CREATE TABLE IF NOT EXISTS purchase_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Purchase order id',
  order_no VARCHAR(40) NOT NULL COMMENT 'Purchase order number',
  demand_id BIGINT NOT NULL COMMENT 'Material demand id',
  demand_no VARCHAR(40) NOT NULL COMMENT 'Material demand number snapshot',
  application_no VARCHAR(80) NULL COMMENT 'Application number snapshot',
  buyer_company_id BIGINT NOT NULL COMMENT 'Buyer company id',
  buyer_company_name VARCHAR(200) NULL COMMENT 'Buyer company name snapshot',
  vessel_name VARCHAR(160) NULL COMMENT 'Vessel name snapshot',
  supply_port VARCHAR(160) NULL COMMENT 'Supply port',
  vessel_eta VARCHAR(40) NULL COMMENT 'Vessel ETA',
  required_delivery_time VARCHAR(40) NULL COMMENT 'Required delivery time',
  strategy_type VARCHAR(40) NOT NULL COMMENT 'LOWEST_MIXED, SINGLE_SUPPLIER',
  strategy_name VARCHAR(80) NOT NULL COMMENT 'Strategy display name',
  supplier_count INT NOT NULL DEFAULT 0 COMMENT 'Supplier sub-order count',
  item_count INT NOT NULL DEFAULT 0 COMMENT 'Order item count',
  total_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT 'Total amount',
  currency VARCHAR(20) NOT NULL DEFAULT 'CNY' COMMENT 'Currency',
  status VARCHAR(40) NOT NULL COMMENT 'PENDING_SUPPLIER_CONFIRM, PARTIALLY_CONFIRMED, PARTIALLY_REJECTED, PREPARING, REJECTED, CANCELLED',
  buyer_remark VARCHAR(1000) NULL COMMENT 'Buyer remark',
  created_by BIGINT NULL COMMENT 'Buyer user id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_purchase_order_no (order_no),
  KEY idx_purchase_order_buyer_status (buyer_company_id, status),
  KEY idx_purchase_order_demand_strategy (buyer_company_id, demand_id, strategy_type),
  KEY idx_purchase_order_created (created_at),
  CONSTRAINT fk_purchase_order_demand
    FOREIGN KEY (demand_id) REFERENCES material_demand (id),
  CONSTRAINT fk_purchase_order_buyer_company
    FOREIGN KEY (buyer_company_id) REFERENCES company (id),
  CONSTRAINT fk_purchase_order_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Material purchase order master';

CREATE TABLE IF NOT EXISTS purchase_order_supplier (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Supplier sub-order id',
  supplier_order_no VARCHAR(50) NOT NULL COMMENT 'Supplier order number',
  order_id BIGINT NOT NULL COMMENT 'Purchase order id',
  supplier_company_id BIGINT NOT NULL COMMENT 'Supplier company id',
  supplier_name VARCHAR(200) NULL COMMENT 'Supplier name snapshot',
  status VARCHAR(40) NOT NULL COMMENT 'PENDING_SUPPLIER_CONFIRM, PREPARING, REJECTED',
  item_count INT NOT NULL DEFAULT 0 COMMENT 'Item count',
  subtotal_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT 'Subtotal amount',
  discount_type VARCHAR(30) NULL COMMENT 'AMOUNT, PERCENT',
  discount_value DECIMAL(18,4) NULL COMMENT 'Discount input value',
  discount_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT 'Discount amount',
  final_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT 'Final amount',
  currency VARCHAR(20) NOT NULL DEFAULT 'CNY' COMMENT 'Currency',
  packaging_method VARCHAR(200) NULL COMMENT 'Packaging method',
  expected_ready_at VARCHAR(40) NULL COMMENT 'Expected ready time',
  supplier_remark VARCHAR(1000) NULL COMMENT 'Supplier remark',
  reject_reason VARCHAR(1000) NULL COMMENT 'Reject reason',
  confirmed_at DATETIME NULL COMMENT 'Confirmed at',
  rejected_at DATETIME NULL COMMENT 'Rejected at',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_purchase_supplier_order_no (supplier_order_no),
  KEY idx_purchase_supplier_order (order_id, status),
  KEY idx_purchase_supplier_company_status (supplier_company_id, status),
  CONSTRAINT fk_purchase_supplier_order
    FOREIGN KEY (order_id) REFERENCES purchase_order (id) ON DELETE CASCADE,
  CONSTRAINT fk_purchase_supplier_company
    FOREIGN KEY (supplier_company_id) REFERENCES company (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Material purchase supplier sub-order';

CREATE TABLE IF NOT EXISTS purchase_order_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Purchase order item id',
  supplier_order_id BIGINT NOT NULL COMMENT 'Supplier sub-order id',
  order_id BIGINT NOT NULL COMMENT 'Purchase order id',
  demand_item_id BIGINT NOT NULL COMMENT 'Material demand item id',
  sku_id BIGINT NOT NULL COMMENT 'Supplier SKU id',
  supplier_sku_code VARCHAR(120) NULL COMMENT 'Supplier SKU code snapshot',
  platform_code VARCHAR(100) NULL COMMENT 'Platform code snapshot',
  impa_code VARCHAR(80) NULL COMMENT 'IMPA code snapshot',
  product_name VARCHAR(500) NOT NULL COMMENT 'Product name snapshot',
  specification VARCHAR(1000) NULL COMMENT 'Specification snapshot',
  quantity VARCHAR(80) NULL COMMENT 'Original demand quantity',
  unit VARCHAR(80) NULL COMMENT 'Demand unit',
  pricing_quantity DECIMAL(18,4) NOT NULL DEFAULT 1 COMMENT 'Pricing quantity',
  unit_price DECIMAL(18,4) NOT NULL COMMENT 'Unit price snapshot',
  amount DECIMAL(18,4) NOT NULL COMMENT 'Line amount',
  currency VARCHAR(20) NOT NULL DEFAULT 'CNY' COMMENT 'Currency',
  unit_mismatch_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether unit mismatch needs confirmation',
  quantity_fallback_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether pricing quantity fell back to 1',
  source_match_type VARCHAR(60) NULL COMMENT 'Comparison candidate match type',
  source_reason VARCHAR(120) NULL COMMENT 'Comparison candidate reason',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  KEY idx_purchase_order_item_supplier (supplier_order_id),
  KEY idx_purchase_order_item_order (order_id),
  KEY idx_purchase_order_item_sku (sku_id),
  KEY idx_purchase_order_item_demand_item (demand_item_id),
  CONSTRAINT fk_purchase_item_supplier_order
    FOREIGN KEY (supplier_order_id) REFERENCES purchase_order_supplier (id) ON DELETE CASCADE,
  CONSTRAINT fk_purchase_item_order
    FOREIGN KEY (order_id) REFERENCES purchase_order (id) ON DELETE CASCADE,
  CONSTRAINT fk_purchase_item_demand_item
    FOREIGN KEY (demand_item_id) REFERENCES material_demand_item (id),
  CONSTRAINT fk_purchase_item_sku
    FOREIGN KEY (sku_id) REFERENCES shop_sku (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Material purchase order item';

CREATE TABLE IF NOT EXISTS purchase_order_event (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Purchase order event id',
  order_id BIGINT NOT NULL COMMENT 'Purchase order id',
  supplier_order_id BIGINT NULL COMMENT 'Supplier sub-order id',
  event_type VARCHAR(60) NOT NULL COMMENT 'ORDER_CREATED, SUPPLIER_ORDER_CREATED, SUPPLIER_CONFIRMED, SUPPLIER_REJECTED, STATUS_UPDATED',
  event_message VARCHAR(1000) NULL COMMENT 'Event message',
  operator_user_id BIGINT NULL COMMENT 'Operator user id',
  operator_company_id BIGINT NULL COMMENT 'Operator company id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  KEY idx_purchase_event_order_time (order_id, created_at),
  KEY idx_purchase_event_supplier_order (supplier_order_id),
  CONSTRAINT fk_purchase_event_order
    FOREIGN KEY (order_id) REFERENCES purchase_order (id) ON DELETE CASCADE,
  CONSTRAINT fk_purchase_event_supplier_order
    FOREIGN KEY (supplier_order_id) REFERENCES purchase_order_supplier (id) ON DELETE SET NULL,
  CONSTRAINT fk_purchase_event_operator_user
    FOREIGN KEY (operator_user_id) REFERENCES sys_user (id) ON DELETE SET NULL,
  CONSTRAINT fk_purchase_event_operator_company
    FOREIGN KEY (operator_company_id) REFERENCES company (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Material purchase order timeline events';
