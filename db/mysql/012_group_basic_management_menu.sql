-- Group basic management menus under a non-clickable parent menu.
-- Idempotent: does not clear role permissions, user menu customizations, or existing permissions.

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

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.enabled = 1
 AND (menu.route_path = '' OR menu.route_path NOT LIKE '/admin/%')
 AND menu.menu_code NOT IN ('ONBOARDING_PROFILE', 'ONBOARDING_REVIEW_STATUS', 'BASIC_MANAGEMENT')
JOIN sys_permission permission
  ON permission.permission_code = menu.required_permission
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
JOIN sys_menu menu
  ON menu.enabled = 1
JOIN sys_permission permission
  ON permission.permission_code = menu.required_permission
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NULL
  AND role.role_code = 'PLATFORM_ADMIN';
