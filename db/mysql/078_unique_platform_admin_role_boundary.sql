-- Keep the global platform-administrator boundary assigned only to the canonical admin account.
-- Password rotation and session revocation are intentionally handled outside this migration.

DELETE user_role
FROM sys_user_role user_role
JOIN sys_role role ON role.id = user_role.role_id
JOIN sys_user user_account ON user_account.id = user_role.user_id
WHERE (role.role_code = 'PLATFORM_ADMIN' OR role.role_type = 'PLATFORM_ADMIN')
  AND user_account.username <> 'admin';

DELETE user_menu
FROM sys_user_menu user_menu
JOIN sys_user user_account ON user_account.id = user_menu.user_id
JOIN sys_menu menu_item ON menu_item.id = user_menu.menu_id
WHERE user_account.username <> 'admin'
  AND REPLACE(COALESCE(menu_item.visible_roles, ''), ' ', '') = 'PLATFORM_ADMIN';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user_account.id, role.id
FROM sys_user user_account
JOIN sys_role role
  ON role.role_code = 'PLATFORM_ADMIN'
 AND role.role_type = 'PLATFORM_ADMIN'
 AND role.company_id IS NULL
 AND role.enabled = 1
WHERE user_account.username = 'admin'
  AND user_account.user_type = 'PLATFORM_ADMIN'
  AND user_account.status = 'ACTIVE';
