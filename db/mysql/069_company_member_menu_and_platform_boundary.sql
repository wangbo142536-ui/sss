-- Keep enterprise member management inside basic services and reserve it for enterprise administrators.
-- Platform administrators govern cross-company accounts through /admin/permissions.

UPDATE sys_menu
SET parent_code = 'BASIC_MANAGEMENT',
    sort_order = 70,
    visible_roles = 'SHIP_AGENT,SUPPLIER,BARGE_AGENT'
WHERE menu_code = 'COMPANY_MEMBERS';

DELETE role_permission
FROM sys_role_permission role_permission
JOIN sys_role role ON role.id = role_permission.role_id
JOIN sys_permission permission ON permission.id = role_permission.permission_id
WHERE role.company_id IS NULL
  AND role.role_code = 'PLATFORM_ADMIN'
  AND permission.permission_code = 'COMPANY_MEMBERS_VIEW';

DELETE user_menu
FROM sys_user_menu user_menu
JOIN sys_menu menu ON menu.id = user_menu.menu_id
JOIN sys_user_role user_role ON user_role.user_id = user_menu.user_id
JOIN sys_role role ON role.id = user_role.role_id
WHERE menu.menu_code = 'COMPANY_MEMBERS'
  AND role.company_id IS NULL
  AND role.role_code = 'PLATFORM_ADMIN';
