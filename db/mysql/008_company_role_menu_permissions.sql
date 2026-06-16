SET @has_full_name := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'full_name'
);
SET @add_full_name_sql := IF(
  @has_full_name = 0,
  'ALTER TABLE sys_user ADD COLUMN full_name VARCHAR(100) NULL COMMENT ''姓名'' AFTER username',
  'SELECT ''sys_user.full_name already exists'''
);
PREPARE add_full_name_stmt FROM @add_full_name_sql;
EXECUTE add_full_name_stmt;
DEALLOCATE PREPARE add_full_name_stmt;

SET @has_email := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'email'
);
SET @add_email_sql := IF(
  @has_email = 0,
  'ALTER TABLE sys_user ADD COLUMN email VARCHAR(160) NULL COMMENT ''邮箱'' AFTER phone',
  'SELECT ''sys_user.email already exists'''
);
PREPARE add_email_stmt FROM @add_email_sql;
EXECUTE add_email_stmt;
DEALLOCATE PREPARE add_email_stmt;

SET @has_role_company := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_role'
    AND column_name = 'company_id'
);
SET @add_role_company_sql := IF(
  @has_role_company = 0,
  'ALTER TABLE sys_role ADD COLUMN company_id BIGINT NULL COMMENT ''所属企业ID，NULL表示平台/全局角色'' AFTER id',
  'SELECT ''sys_role.company_id already exists'''
);
PREPARE add_role_company_stmt FROM @add_role_company_sql;
EXECUTE add_role_company_stmt;
DEALLOCATE PREPARE add_role_company_stmt;

SET @has_role_company_index := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_role'
    AND index_name = 'idx_sys_role_company'
);
SET @add_role_company_index_sql := IF(
  @has_role_company_index = 0,
  'ALTER TABLE sys_role ADD INDEX idx_sys_role_company (company_id)',
  'SELECT ''sys_role.idx_sys_role_company already exists'''
);
PREPARE add_role_company_index_stmt FROM @add_role_company_index_sql;
EXECUTE add_role_company_index_stmt;
DEALLOCATE PREPARE add_role_company_index_stmt;

CREATE TABLE IF NOT EXISTS sys_user_menu (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_menu (user_id, menu_id),
  KEY idx_user_menu_menu (menu_id),
  CONSTRAINT fk_user_menu_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_menu_menu
    FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户菜单权限';

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('COMPANY_MEMBERS_VIEW', '企业成员管理查看', 'MENU', 'COMPANY_MEMBERS', 'VIEW', 1)
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
VALUES
  ('COMPANY_MEMBERS', '企业成员管理', '/company/members', 'UsersRound', NULL, 100, 'COMPANY_MEMBERS_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT', 1)
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
JOIN sys_permission permission ON permission.permission_code = 'COMPANY_MEMBERS_VIEW'
WHERE role.role_code IN ('SHIP_AGENT', 'SUPPLIER', 'BARGE_AGENT', 'PLATFORM_ADMIN')
  AND role.company_id IS NULL
  AND permission.enabled = 1;

INSERT INTO sys_role (company_id, role_code, role_name, role_type, enabled)
SELECT company.id, CONCAT('COMPANY_ADMIN_', company.id), '企业管理员', 'COMPANY_ADMIN', 1
FROM company
WHERE company.status = 'ACTIVE'
  AND company.company_type <> 'PLATFORM_ADMIN'
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  role_type = VALUES(role_type),
  enabled = 1;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user.id, role.id
FROM sys_user user
JOIN sys_role role
  ON role.company_id = user.company_id
 AND role.role_code = CONCAT('COMPANY_ADMIN_', user.company_id)
WHERE user.is_company_owner = 1
  AND user.status = 'ACTIVE'
  AND role.enabled = 1;

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
WHERE role.company_id IS NOT NULL
  AND role.role_code = CONCAT('COMPANY_ADMIN_', role.company_id)
  AND role.enabled = 1;
