-- Add crew service menu and rename delivery/settlement operation menus.
-- Idempotent: only inserts/updates sys_permission, sys_menu, and missing admin role permissions.
-- It does not clear role permissions, user menu customizations, or business data.

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('CREW_SERVICE_VIEW', '船员服务查看', 'MENU', 'CREW_SERVICE', 'VIEW', 1)
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_type = VALUES(permission_type),
  resource_code = VALUES(resource_code),
  action_code = VALUES(action_code),
  enabled = VALUES(enabled);

UPDATE sys_permission
SET permission_name = '驳船管理查看'
WHERE permission_code = 'DELIVERY_TASK_VIEW';

UPDATE sys_permission
SET permission_name = '结算管理查看'
WHERE permission_code = 'SETTLEMENT_VIEW';

INSERT INTO sys_menu (
  menu_code, menu_name, route_path, icon, parent_code, sort_order,
  required_permission, visible_roles, enabled
)
VALUES (
  'CREW_SERVICE', '船员服务', '/crew-services', 'UsersRound', NULL, 30,
  'CREW_SERVICE_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1
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
SET menu_name = '驳船管理'
WHERE menu_code = 'DELIVERY_TASKS';

UPDATE sys_menu
SET menu_name = '结算管理'
WHERE menu_code = 'SETTLEMENT';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'CREW_SERVICE_VIEW'
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NULL
  AND role.role_code IN ('SHIP_AGENT', 'SUPPLIER', 'BARGE_AGENT', 'PLATFORM_ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'CREW_SERVICE_VIEW'
 AND permission.enabled = 1
WHERE role.enabled = 1
  AND role.company_id IS NOT NULL
  AND (
    role.role_type = 'COMPANY_ADMIN'
    OR role.role_code = CONCAT('COMPANY_ADMIN_', role.company_id)
  );
