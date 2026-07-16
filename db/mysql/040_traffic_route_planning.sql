CREATE TABLE IF NOT EXISTS traffic_route_plan (
  id BIGINT NOT NULL AUTO_INCREMENT,
  route_no VARCHAR(40) NOT NULL,
  route_name VARCHAR(120) NOT NULL,
  service_date DATE NOT NULL,
  sea_area VARCHAR(20) NULL,
  supplier_company_id BIGINT NULL,
  supplier_company_name VARCHAR(160) NULL,
  traffic_vessel_id BIGINT NULL,
  traffic_vessel_name VARCHAR(160) NULL,
  planned_departure_time DATETIME NULL,
  planned_finish_time DATETIME NULL,
  allow_share TINYINT(1) NOT NULL DEFAULT 1,
  order_count INT NOT NULL DEFAULT 0,
  total_income DECIMAL(14,2) NULL,
  estimated_cost DECIMAL(14,2) NULL,
  estimated_profit DECIMAL(14,2) NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  remark VARCHAR(1000) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_traffic_route_no (route_no),
  KEY idx_traffic_route_date (service_date, status),
  KEY idx_traffic_route_supplier (supplier_company_id, service_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Traffic route plans';

CREATE TABLE IF NOT EXISTS traffic_route_stop (
  id BIGINT NOT NULL AUTO_INCREMENT,
  route_plan_id BIGINT NOT NULL,
  traffic_service_order_id BIGINT NOT NULL,
  stop_sequence INT NOT NULL DEFAULT 1,
  anchorage_code VARCHAR(80) NULL,
  anchorage_name VARCHAR(160) NULL,
  planned_service_time DATETIME NULL,
  service_type VARCHAR(40) NULL,
  contact_name VARCHAR(120) NULL,
  contact_phone VARCHAR(80) NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'PLANNED',
  remark VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_traffic_route_stop_order (traffic_service_order_id),
  KEY idx_traffic_route_stop_plan (route_plan_id, stop_sequence),
  CONSTRAINT fk_traffic_route_stop_plan FOREIGN KEY (route_plan_id) REFERENCES traffic_route_plan (id) ON DELETE CASCADE,
  CONSTRAINT fk_traffic_route_stop_order FOREIGN KEY (traffic_service_order_id) REFERENCES traffic_service_order (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Traffic route stops';

CREATE TABLE IF NOT EXISTS traffic_route_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  route_plan_id BIGINT NOT NULL,
  traffic_service_order_id BIGINT NULL,
  event_type VARCHAR(60) NOT NULL,
  event_message VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_traffic_route_event_plan (route_plan_id, created_at),
  CONSTRAINT fk_traffic_route_event_plan FOREIGN KEY (route_plan_id) REFERENCES traffic_route_plan (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Traffic route events';

SET @add_route_plan_id := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN route_plan_id BIGINT NULL AFTER supplier_company_name',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'traffic_service_order' AND COLUMN_NAME = 'route_plan_id'
);
PREPARE stmt FROM @add_route_plan_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_route_stop_id := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN route_stop_id BIGINT NULL AFTER route_plan_id',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'traffic_service_order' AND COLUMN_NAME = 'route_stop_id'
);
PREPARE stmt FROM @add_route_stop_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_planned_sequence := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN planned_sequence INT NULL AFTER route_stop_id',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'traffic_service_order' AND COLUMN_NAME = 'planned_sequence'
);
PREPARE stmt FROM @add_planned_sequence;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_planned_service_time := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE traffic_service_order ADD COLUMN planned_service_time DATETIME NULL AFTER planned_sequence',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'traffic_service_order' AND COLUMN_NAME = 'planned_service_time'
);
PREPARE stmt FROM @add_planned_service_time;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled) VALUES
  ('TRAFFIC_ROUTE_VIEW', '路线规划查看', 'MENU', 'TRAFFIC_ROUTE_PLANNING', 'VIEW', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), resource_code = VALUES(resource_code), action_code = VALUES(action_code), enabled = VALUES(enabled);

INSERT INTO sys_menu (
  menu_code,
  menu_name,
  route_path,
  icon,
  parent_code,
  sort_order,
  required_permission,
  visible_roles,
  enabled
) VALUES
  ('TRAFFIC_ROUTE_PLANNING', '路线规划', '/traffic-routes', 'Route', 'TRAFFIC_SERVICE', 20, 'TRAFFIC_ROUTE_VIEW', 'SHIP_AGENT,SUPPLIER,PLATFORM_ADMIN', 1)
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
JOIN sys_permission permission ON permission.permission_code = 'TRAFFIC_ROUTE_VIEW'
WHERE role.role_code IN ('SHIP_AGENT', 'SUPPLIER', 'PLATFORM_ADMIN')
   OR role.role_type = 'COMPANY_ADMIN'
   OR role.role_code LIKE 'COMPANY_ADMIN\_%';
