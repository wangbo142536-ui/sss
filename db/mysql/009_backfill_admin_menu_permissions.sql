-- Backfill menu permissions for historical admin roles.
-- This script is idempotent and only inserts missing role-permission rows.
-- It does not clear data and does not touch per-user menu customizations.

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.enabled = 1
 AND menu.route_path NOT LIKE '/admin/%'
 AND menu.menu_code NOT IN ('ONBOARDING_PROFILE', 'ONBOARDING_REVIEW_STATUS')
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
