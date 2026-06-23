-- Material procurement multi-unit price and supplier fulfillment extension.
-- Idempotent migration. Scope: material SKU comparison and purchase fulfillment only.

CREATE TABLE IF NOT EXISTS shop_sku_unit_price (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SKU unit price id',
  sku_id BIGINT NOT NULL COMMENT 'Supplier SKU id',
  unit VARCHAR(80) NOT NULL COMMENT 'Sales/pricing unit',
  unit_price_cny DECIMAL(18,4) NOT NULL COMMENT 'CNY unit price',
  unit_price_usd DECIMAL(18,4) NOT NULL COMMENT 'USD unit price, CNY / 7',
  is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Default comparison unit',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether option can be selected',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Lowest unit should sort first',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shop_sku_unit_price (sku_id, unit),
  KEY idx_shop_sku_unit_price_sku_enabled (sku_id, enabled, sort_order),
  CONSTRAINT fk_shop_sku_unit_price_sku
    FOREIGN KEY (sku_id) REFERENCES shop_sku (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Supplier SKU multi-unit prices';

INSERT INTO shop_sku_unit_price
  (sku_id, unit, unit_price_cny, unit_price_usd, is_default, enabled, sort_order)
SELECT s.id,
       COALESCE(NULLIF(s.unit, ''), NULLIF(s.stock_unit, ''), '个') AS unit,
       s.unit_price,
       ROUND(s.unit_price / 7, 4),
       1,
       1,
       0
FROM shop_sku s
WHERE s.product_type = 'MATERIAL'
  AND s.unit_price IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM shop_sku_unit_price p
    WHERE p.sku_id = s.id
  );

SET @schema_name := DATABASE();

SET @has_purchase_order_total_usd := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order' AND column_name = 'total_amount_usd'
);
SET @sql := IF(@has_purchase_order_total_usd = 0,
  'ALTER TABLE purchase_order ADD COLUMN total_amount_usd DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT ''USD total amount'' AFTER total_amount',
  'SELECT ''purchase_order.total_amount_usd already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_supplier_subtotal_usd := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_supplier' AND column_name = 'subtotal_amount_usd'
);
SET @sql := IF(@has_supplier_subtotal_usd = 0,
  'ALTER TABLE purchase_order_supplier ADD COLUMN subtotal_amount_usd DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT ''USD subtotal amount'' AFTER subtotal_amount',
  'SELECT ''purchase_order_supplier.subtotal_amount_usd already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_supplier_final_usd := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_supplier' AND column_name = 'final_amount_usd'
);
SET @sql := IF(@has_supplier_final_usd = 0,
  'ALTER TABLE purchase_order_supplier ADD COLUMN final_amount_usd DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT ''USD final amount'' AFTER final_amount',
  'SELECT ''purchase_order_supplier.final_amount_usd already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_ready_at := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_supplier' AND column_name = 'ready_at'
);
SET @sql := IF(@has_ready_at = 0,
  'ALTER TABLE purchase_order_supplier ADD COLUMN ready_at DATETIME NULL COMMENT ''Ready to deliver time'' AFTER confirmed_at',
  'SELECT ''purchase_order_supplier.ready_at already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_supplied_at := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_supplier' AND column_name = 'supplied_at'
);
SET @sql := IF(@has_supplied_at = 0,
  'ALTER TABLE purchase_order_supplier ADD COLUMN supplied_at DATETIME NULL COMMENT ''Supplied to vessel time'' AFTER ready_at',
  'SELECT ''purchase_order_supplier.supplied_at already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_delivery_image_url := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_supplier' AND column_name = 'delivery_image_url'
);
SET @sql := IF(@has_delivery_image_url = 0,
  'ALTER TABLE purchase_order_supplier ADD COLUMN delivery_image_url VARCHAR(500) NULL COMMENT ''Delivery proof image URL'' AFTER supplied_at',
  'SELECT ''purchase_order_supplier.delivery_image_url already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_delivery_image_file_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_supplier' AND column_name = 'delivery_image_file_id'
);
SET @sql := IF(@has_delivery_image_file_id = 0,
  'ALTER TABLE purchase_order_supplier ADD COLUMN delivery_image_file_id VARCHAR(80) NULL COMMENT ''Delivery proof file id'' AFTER delivery_image_url',
  'SELECT ''purchase_order_supplier.delivery_image_file_id already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_delivery_remark := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_supplier' AND column_name = 'delivery_remark'
);
SET @sql := IF(@has_delivery_remark = 0,
  'ALTER TABLE purchase_order_supplier ADD COLUMN delivery_remark VARCHAR(1000) NULL COMMENT ''Delivery completion remark'' AFTER delivery_image_file_id',
  'SELECT ''purchase_order_supplier.delivery_remark already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_item_unit_price_usd := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_item' AND column_name = 'unit_price_usd'
);
SET @sql := IF(@has_item_unit_price_usd = 0,
  'ALTER TABLE purchase_order_item ADD COLUMN unit_price_usd DECIMAL(18,4) NULL COMMENT ''USD unit price snapshot'' AFTER unit_price',
  'SELECT ''purchase_order_item.unit_price_usd already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_item_amount_usd := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'purchase_order_item' AND column_name = 'amount_usd'
);
SET @sql := IF(@has_item_amount_usd = 0,
  'ALTER TABLE purchase_order_item ADD COLUMN amount_usd DECIMAL(18,4) NULL COMMENT ''USD line amount snapshot'' AFTER amount',
  'SELECT ''purchase_order_item.amount_usd already exists'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
