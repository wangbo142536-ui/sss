SET @schema_name := DATABASE();

SET @has_demand_traffic_json := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE table_schema = @schema_name AND table_name = 'material_demand' AND column_name = 'traffic_service_json'
);
SET @sql := IF(@has_demand_traffic_json = 0,
  'ALTER TABLE material_demand ADD COLUMN traffic_service_json JSON NULL COMMENT ''Traffic service form snapshot'' AFTER fixed_other_fee',
  'SELECT ''material_demand.traffic_service_json already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_traffic_demand_id := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'demand_id'
);
SET @sql := IF(@has_traffic_demand_id = 0,
  'ALTER TABLE traffic_service_order ADD COLUMN demand_id BIGINT NULL COMMENT ''Source material demand id'' AFTER requester_company_id',
  'SELECT ''traffic_service_order.demand_id already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_traffic_purchase_order_id := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'purchase_order_id'
);
SET @sql := IF(@has_traffic_purchase_order_id = 0,
  'ALTER TABLE traffic_service_order ADD COLUMN purchase_order_id BIGINT NULL COMMENT ''Linked purchase order id'' AFTER demand_id',
  'SELECT ''traffic_service_order.purchase_order_id already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_traffic_supplier_company_id := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'supplier_company_id'
);
SET @sql := IF(@has_traffic_supplier_company_id = 0,
  'ALTER TABLE traffic_service_order ADD COLUMN supplier_company_id BIGINT NULL COMMENT ''Assigned traffic service supplier company id'' AFTER purchase_order_id',
  'SELECT ''traffic_service_order.supplier_company_id already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_traffic_supplier_company_name := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'supplier_company_name'
);
SET @sql := IF(@has_traffic_supplier_company_name = 0,
  'ALTER TABLE traffic_service_order ADD COLUMN supplier_company_name VARCHAR(180) NULL COMMENT ''Assigned traffic service supplier name'' AFTER supplier_company_id',
  'SELECT ''traffic_service_order.supplier_company_name already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_traffic_link_index := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND index_name = 'idx_traffic_service_source'
);
SET @sql := IF(@has_traffic_link_index = 0,
  'CREATE INDEX idx_traffic_service_source ON traffic_service_order (requester_company_id, demand_id, purchase_order_id)',
  'SELECT ''idx_traffic_service_source already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_menu
SET menu_name = '三方-交通服务'
WHERE menu_code = 'TRAFFIC_BOAT';
