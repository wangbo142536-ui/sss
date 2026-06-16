-- Create shop management menu for supplier/company admins.
-- Idempotent: only inserts/updates sys_permission, sys_menu, and missing role permissions.
-- It does not clear role permissions, user menu customizations, or business data.

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('SHOP_MANAGEMENT_VIEW', '店铺管理查看', 'MENU', 'SHOP_MANAGEMENT', 'VIEW', 1)
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
  'SHOP_MANAGEMENT', '店铺管理', '/shop/products', 'Store', NULL, 25,
  'SHOP_MANAGEMENT_VIEW', 'SUPPLIER,SHIP_AGENT,PLATFORM_ADMIN', 1
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

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'SHOP_MANAGEMENT_VIEW'
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NULL
  AND role.role_code IN ('SUPPLIER', 'PLATFORM_ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'SHOP_MANAGEMENT_VIEW'
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NOT NULL
  AND (
    role.role_type = 'COMPANY_ADMIN'
    OR role.role_code = CONCAT('COMPANY_ADMIN_', role.company_id)
  );
