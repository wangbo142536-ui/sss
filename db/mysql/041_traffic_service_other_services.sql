SET @schema_name := DATABASE();

SET @add_traffic_fee_type := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN fee_type VARCHAR(32) NOT NULL DEFAULT ''FREIGHT'' AFTER supplier_company_name',
    'SELECT ''traffic_service_order.fee_type exists'''
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'traffic_service_order' AND column_name = 'fee_type'
);
PREPARE stmt FROM @add_traffic_fee_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE traffic_service_order
SET fee_type = 'FREIGHT'
WHERE fee_type IS NULL OR fee_type = '';

UPDATE sys_menu
SET menu_name = '其他服务'
WHERE menu_code IN ('TRAFFIC_SERVICE', 'DELIVERY_TASKS');

UPDATE sys_menu
SET menu_name = '交通艇服务'
WHERE menu_code = 'TRAFFIC_BOAT';

UPDATE sys_menu
SET menu_name = '订单规划'
WHERE menu_code = 'TRAFFIC_ROUTE_PLANNING';
