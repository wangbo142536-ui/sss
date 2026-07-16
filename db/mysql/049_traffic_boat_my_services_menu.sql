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
) VALUES (
  'TRAFFIC_BOAT_MY_SERVICES',
  '我的服务',
  '/traffic-boat/my-services',
  'ClipboardList',
  'TRAFFIC_SERVICE',
  15,
  'TRAFFIC_BOAT_VIEW',
  'SUPPLIER,PLATFORM_ADMIN,SHIP_AGENT',
  1
) ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  route_path = VALUES(route_path),
  icon = VALUES(icon),
  parent_code = VALUES(parent_code),
  sort_order = VALUES(sort_order),
  required_permission = VALUES(required_permission),
  visible_roles = VALUES(visible_roles),
  enabled = VALUES(enabled);
