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
  ('TRAFFIC_SERVICE', '交通服务', '', 'Ship', NULL, 90, 'TRAFFIC_SERVICE_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  route_path = VALUES(route_path),
  icon = VALUES(icon),
  parent_code = VALUES(parent_code),
  sort_order = VALUES(sort_order),
  required_permission = VALUES(required_permission),
  visible_roles = VALUES(visible_roles),
  enabled = VALUES(enabled);

UPDATE sys_menu
SET menu_name = '运输服务',
    route_path = '/transport/services',
    icon = 'Ship',
    parent_code = 'TRAFFIC_SERVICE',
    sort_order = 0,
    required_permission = 'TRAFFIC_SERVICE_VIEW',
    visible_roles = 'SHIP_AGENT,SUPPLIER,PLATFORM_ADMIN',
    enabled = 1
WHERE menu_code = 'DELIVERY_TASKS';

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
  ('TRAFFIC_BOAT', '交通艇', '/traffic-boat', 'ShipWheel', 'TRAFFIC_SERVICE', 10, 'TRAFFIC_BOAT_VIEW', 'SUPPLIER,PLATFORM_ADMIN,SHIP_AGENT', 1)
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
JOIN sys_permission permission ON permission.permission_code IN ('TRAFFIC_SERVICE_VIEW', 'TRAFFIC_BOAT_VIEW')
WHERE role.role_code IN ('SHIP_AGENT', 'SUPPLIER', 'BARGE_AGENT', 'PLATFORM_ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission ON permission.permission_code IN ('TRAFFIC_SERVICE_VIEW', 'TRAFFIC_BOAT_VIEW')
WHERE role.role_type = 'COMPANY_ADMIN'
   OR role.role_code LIKE 'COMPANY_ADMIN\_%';
