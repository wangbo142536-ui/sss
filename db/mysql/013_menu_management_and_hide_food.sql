-- Add menu management entry and hide food procurement menu.
-- Idempotent: only updates sys_menu visibility/structure and inserts missing permissions.
-- It does not clear role permissions, user menu customizations, or business data.

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('ADMIN_MENU_MANAGE', '菜单管理', 'MENU', 'ADMIN_MENU', 'MANAGE', 1)
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
  'BASIC_MANAGEMENT', '基础管理', '', 'Settings', NULL, 110,
  NULL, 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1
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

UPDATE sys_menu
SET parent_code = 'BASIC_MANAGEMENT',
    sort_order = CASE menu_code
      WHEN 'STANDARD_LIBRARY_IMPA' THEN 0
      WHEN 'SUPPLIERS' THEN 10
      WHEN 'ADMIN_REGISTRATIONS' THEN 20
      WHEN 'ADMIN_PERMISSION' THEN 30
      ELSE sort_order
    END
WHERE menu_code IN (
  'STANDARD_LIBRARY_IMPA',
  'SUPPLIERS',
  'ADMIN_REGISTRATIONS',
  'ADMIN_PERMISSION'
);

INSERT INTO sys_menu (
  menu_code, menu_name, route_path, icon, parent_code, sort_order,
  required_permission, visible_roles, enabled
)
VALUES (
  'MENU_MANAGEMENT', '菜单管理', '/admin/menus', 'ListTree', 'BASIC_MANAGEMENT', 40,
  'ADMIN_MENU_MANAGE', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1
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

UPDATE sys_menu
SET enabled = 0
WHERE menu_code = 'PROCUREMENT_FOOD'
   OR route_path = '/procurement/food';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'ADMIN_MENU_MANAGE'
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
  ON permission.permission_code = 'ADMIN_MENU_MANAGE'
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NULL
  AND role.role_code = 'PLATFORM_ADMIN';
