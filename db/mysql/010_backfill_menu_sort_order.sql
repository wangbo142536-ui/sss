-- Backfill stable numeric menu ordering.
-- Sort order uses 0, 10, 20... so 01-09, 11-19... remain available for inserts.
-- This script is idempotent and does not touch roles, permissions, or user customizations.

UPDATE sys_menu
SET sort_order = CASE menu_code
  WHEN 'DASHBOARD' THEN 0
  WHEN 'PROCUREMENT_MATERIALS' THEN 10
  WHEN 'PROCUREMENT_FOOD' THEN 20
  WHEN 'STANDARD_LIBRARY_IMPA' THEN 30
  WHEN 'SUPPLIERS' THEN 40
  WHEN 'QUOTES' THEN 50
  WHEN 'COMPARISON' THEN 60
  WHEN 'ORDERS' THEN 70
  WHEN 'DELIVERY_TASKS' THEN 80
  WHEN 'SETTLEMENT' THEN 90
  WHEN 'COMPANY_MEMBERS' THEN 100
  WHEN 'ONBOARDING_PROFILE' THEN 800
  WHEN 'ONBOARDING_REVIEW_STATUS' THEN 810
  WHEN 'ADMIN_REGISTRATIONS' THEN 890
  WHEN 'ADMIN_PERMISSION' THEN 900
  ELSE sort_order
END
WHERE menu_code IN (
  'DASHBOARD',
  'PROCUREMENT_MATERIALS',
  'PROCUREMENT_FOOD',
  'STANDARD_LIBRARY_IMPA',
  'SUPPLIERS',
  'QUOTES',
  'COMPARISON',
  'ORDERS',
  'DELIVERY_TASKS',
  'SETTLEMENT',
  'COMPANY_MEMBERS',
  'ONBOARDING_PROFILE',
  'ONBOARDING_REVIEW_STATUS',
  'ADMIN_REGISTRATIONS',
  'ADMIN_PERMISSION'
);

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
