CREATE TABLE IF NOT EXISTS traffic_anchorage (
  id BIGINT NOT NULL AUTO_INCREMENT,
  anchorage_code VARCHAR(80) NOT NULL,
  anchorage_name VARCHAR(160) NOT NULL,
  sea_area VARCHAR(20) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_traffic_anchorage_code (anchorage_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Traffic service anchorages';

CREATE TABLE IF NOT EXISTS traffic_service_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  service_no VARCHAR(40) NOT NULL,
  requester_company_id BIGINT NOT NULL,
  sea_area VARCHAR(20) NOT NULL,
  anchorage_code VARCHAR(80) NOT NULL,
  anchorage_name VARCHAR(160) NOT NULL,
  use_time DATETIME NULL,
  service_type VARCHAR(40) NOT NULL DEFAULT 'PERSONNEL',
  passenger_type VARCHAR(40) NOT NULL DEFAULT 'NORMAL',
  passenger_count INT NULL,
  cargo_type VARCHAR(40) NOT NULL DEFAULT 'CARGO',
  return_trip TINYINT(1) NOT NULL DEFAULT 0,
  allow_share TINYINT(1) NOT NULL DEFAULT 0,
  base_price DECIMAL(14,2) NULL,
  shared_price DECIMAL(14,2) NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(1000) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_traffic_service_no (service_no),
  KEY idx_traffic_service_company (requester_company_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Traffic service orders';

CREATE TABLE IF NOT EXISTS traffic_service_cargo (
  id BIGINT NOT NULL AUTO_INCREMENT,
  service_order_id BIGINT NOT NULL,
  cargo_name VARCHAR(160) NOT NULL,
  weight_kg DECIMAL(12,2) NULL,
  volume_cbm DECIMAL(12,2) NULL,
  PRIMARY KEY (id),
  KEY idx_traffic_service_cargo_order (service_order_id),
  CONSTRAINT fk_traffic_service_cargo_order FOREIGN KEY (service_order_id) REFERENCES traffic_service_order (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Traffic service cargo items';

CREATE TABLE IF NOT EXISTS traffic_boat_price (
  id BIGINT NOT NULL AUTO_INCREMENT,
  supplier_company_id BIGINT NOT NULL,
  anchorage_code VARCHAR(80) NOT NULL,
  base_price DECIMAL(14,2) NULL,
  shared_price DECIMAL(14,2) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_traffic_boat_price_supplier_anchor (supplier_company_id, anchorage_code),
  KEY idx_traffic_boat_price_anchor (anchorage_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Traffic boat prices by anchorage';

INSERT IGNORE INTO traffic_anchorage (anchorage_code, anchorage_name, sea_area, sort_order) VALUES
('NORTH_XIUSHAN_EAST', '秀山东锚地', 'NORTH', 10),
('NORTH_QUSHAN_TEMP', '衢山临时锚地', 'NORTH', 20),
('NORTH_XIANGLU_HUAPINGJIAO', '香炉花瓶礁锚地', 'NORTH', 30),
('NORTH_CHANGBAI_ISLAND', '长白岛', 'NORTH', 40),
('NORTH_XIUSHAN_ISLAND', '秀山岛', 'NORTH', 50),
('NORTH_XIUSHAN_EAST_OUTER', '秀山东外锚地', 'NORTH', 60),
('NORTH_WUZHI_NORTH', '五峙北锚地', 'NORTH', 70),
('NORTH_DAISHAN_CHANGHONG_CHANGTU', '岱山长宏船厂（长涂）锚地', 'NORTH', 80),
('NORTH_ZHONGTIAN_SHIPYARD', '中天船厂锚地', 'NORTH', 90),
('NORTH_CHANGHONG_MAIN', '长宏国际船厂（本部）锚地', 'NORTH', 100),
('NORTH_PACIFIC_SHIPYARD', '太平洋船厂锚地', 'NORTH', 110),
('NORTH_QUSHAN_ISLAND', '衢山岛', 'NORTH', 120),
('NORTH_DAISHAN_HUAFENG', '岱山华丰船厂锚地', 'NORTH', 130),
('NORTH_DAISHAN_CHANGHONG_JIANGNANSHAN', '岱山长宏船厂（江南山）锚地', 'NORTH', 140),
('NORTH_DAISHAN_TO_CHANGTU', '岱山本岛到岱山长宏船厂（长涂）锚地', 'NORTH', 150),
('NORTH_JINHAI_HEAVY', '金海重工锚地', 'NORTH', 160),
('NORTH_DAISHAN_TO_JINHAI', '岱山本岛到金海重工', 'NORTH', 170),
('SOUTH_MAZHI_1', '马峙1号锚地', 'SOUTH', 10),
('SOUTH_MAZHI_2', '马峙2号锚地', 'SOUTH', 20),
('SOUTH_AOSHAN_INSPECTION', '岙山联检锚地', 'SOUTH', 30),
('SOUTH_XIAZHIMEN_NORTH', '虾峙门北锚地', 'SOUTH', 40),
('SOUTH_XIAZHIMEN_SOUTH', '虾峙门南锚地', 'SOUTH', 50),
('SOUTH_TIAOZHOUMEN', '条帚门锚地', 'SOUTH', 60),
('SOUTH_LIUHENG_SHIPYARD', '六横船厂锚地', 'SOUTH', 70),
('SOUTH_LAOTANGSHAN_TERMINAL', '老塘山码头', 'SOUTH', 80),
('SOUTH_WANBANG_SHIPYARD', '万邦船厂锚地', 'SOUTH', 90),
('SOUTH_PUTUO_CHANGHONG', '普陀长宏船厂锚地', 'SOUTH', 100),
('SOUTH_LIUHENG_TO_DONGBALIAN', '六横到东白莲码头', 'SOUTH', 110),
('SOUTH_WUGANG_TERMINAL', '武港码头', 'SOUTH', 120),
('SOUTH_LIUHENG_COAL_POWER', '六横煤电码头', 'SOUTH', 130),
('SOUTH_HUATAI_OIL', '华泰油库码头', 'SOUTH', 140),
('SOUTH_JINRUN_TERMINAL', '金润码头', 'SOUTH', 150),
('SOUTH_ZHONGAO_TERMINAL', '中奥码头', 'SOUTH', 160),
('SOUTH_CEZI_OIL', '册子油库码头', 'SOUTH', 170),
('SOUTH_WAIDIAO_OIL', '外钓油库码头', 'SOUTH', 180),
('SOUTH_YANTIAN_ZHOUSHAN_STORAGE', '深圳盐田舟山储运码头（原光汇码头）', 'SOUTH', 190),
('SOUTH_AOSHAN_XINGZHONG', '岙山兴中码头', 'SOUTH', 200),
('SOUTH_DADING_OIL', '大鼎油库码头', 'SOUTH', 210),
('SOUTH_XINAO_LNG', '新奥LNG码头', 'SOUTH', 220),
('SOUTH_DENGBU_ISLAND', '登步岛', 'SOUTH', 230);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled) VALUES
  ('TRAFFIC_SERVICE_VIEW', '交通服务查看', 'MENU', 'TRAFFIC_SERVICE', 'VIEW', 1),
  ('TRAFFIC_BOAT_VIEW', '交通艇查看', 'MENU', 'TRAFFIC_BOAT', 'VIEW', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), resource_code = VALUES(resource_code), action_code = VALUES(action_code), enabled = VALUES(enabled);

UPDATE sys_menu
SET menu_name = '交通服务', route_path = '/transport/services', icon = 'Ship', required_permission = 'TRAFFIC_SERVICE_VIEW', visible_roles = 'SHIP_AGENT,PLATFORM_ADMIN', enabled = 1
WHERE menu_code = 'DELIVERY_TASKS';

INSERT INTO sys_menu (menu_code, menu_name, route_path, icon, parent_code, sort_order, required_permission, visible_roles, enabled) VALUES
  ('TRAFFIC_BOAT', '交通艇', '/traffic-boat', 'ShipWheel', NULL, 95, 'TRAFFIC_BOAT_VIEW', 'SUPPLIER,PLATFORM_ADMIN,SHIP_AGENT', 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), route_path = VALUES(route_path), icon = VALUES(icon), parent_code = VALUES(parent_code), sort_order = VALUES(sort_order), required_permission = VALUES(required_permission), visible_roles = VALUES(visible_roles), enabled = VALUES(enabled);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission ON permission.permission_code IN ('TRAFFIC_SERVICE_VIEW', 'TRAFFIC_BOAT_VIEW')
WHERE role.role_code IN ('SHIP_AGENT', 'SUPPLIER', 'BARGE_AGENT', 'PLATFORM_ADMIN');
