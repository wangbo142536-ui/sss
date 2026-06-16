SET @has_contact_email := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'company'
    AND column_name = 'contact_email'
);
SET @add_contact_email_sql := IF(
  @has_contact_email = 0,
  'ALTER TABLE company ADD COLUMN contact_email VARCHAR(160) NULL COMMENT ''联系邮箱'' AFTER contact_phone',
  'SELECT ''company.contact_email already exists'''
);
PREPARE add_contact_email_stmt FROM @add_contact_email_sql;
EXECUTE add_contact_email_stmt;
DEALLOCATE PREPARE add_contact_email_stmt;

SET @has_review_comment := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'company'
    AND column_name = 'review_comment'
);
SET @add_review_comment_sql := IF(
  @has_review_comment = 0,
  'ALTER TABLE company ADD COLUMN review_comment VARCHAR(500) NULL COMMENT ''审核意见'' AFTER status',
  'SELECT ''company.review_comment already exists'''
);
PREPARE add_review_comment_stmt FROM @add_review_comment_sql;
EXECUTE add_review_comment_stmt;
DEALLOCATE PREPARE add_review_comment_stmt;

CREATE TABLE IF NOT EXISTS sys_file (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  file_id VARCHAR(80) NOT NULL COMMENT '业务文件ID',
  uploader_user_id BIGINT NULL COMMENT '上传用户ID',
  original_name VARCHAR(255) NOT NULL COMMENT '原文件名',
  storage_path VARCHAR(500) NOT NULL COMMENT '存储路径',
  content_type VARCHAR(120) NULL COMMENT '文件类型',
  file_size BIGINT NULL COMMENT '文件大小',
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_file_file_id (file_id),
  KEY idx_sys_file_uploader (uploader_user_id),
  KEY idx_sys_file_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统文件';

CREATE TABLE IF NOT EXISTS company_qualification (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '资质ID',
  company_id BIGINT NOT NULL COMMENT '企业ID',
  file_id VARCHAR(80) NOT NULL COMMENT '业务文件ID',
  file_type VARCHAR(80) NOT NULL COMMENT '资质类型',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_company_qualification_company (company_id),
  KEY idx_company_qualification_file (file_id),
  CONSTRAINT fk_company_qualification_company
    FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业资质文件';

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('ONBOARDING_PROFILE_VIEW', '入驻资料查看', 'MENU', 'ONBOARDING_PROFILE', 'VIEW', 1),
  ('ONBOARDING_REVIEW_STATUS_VIEW', '入驻审核状态查看', 'MENU', 'ONBOARDING_REVIEW_STATUS', 'VIEW', 1),
  ('ADMIN_REGISTRATION_REVIEW', '入驻审核管理', 'MENU', 'ADMIN_REGISTRATION', 'REVIEW', 1),
  ('FILE_UPLOAD', '文件上传', 'ACTION', 'FILE', 'UPLOAD', 1)
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
  ('ONBOARDING_PROFILE', '企业入驻资料', '/onboarding/company-profile', 'ClipboardList', NULL, 800, 'ONBOARDING_PROFILE_VIEW', 'SHIP_AGENT,SUPPLIER', 1),
  ('ONBOARDING_REVIEW_STATUS', '入驻审核状态', '/onboarding/review-status', 'Hourglass', NULL, 810, 'ONBOARDING_REVIEW_STATUS_VIEW', 'SHIP_AGENT,SUPPLIER', 1),
  ('ADMIN_REGISTRATIONS', '入驻审核', '/admin/registrations', 'UserCheck', NULL, 890, 'ADMIN_REGISTRATION_REVIEW', 'PLATFORM_ADMIN', 1)
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
CROSS JOIN sys_permission permission
WHERE role.role_code IN ('SHIP_AGENT', 'SUPPLIER', 'BARGE_AGENT', 'PLATFORM_ADMIN')
  AND permission.enabled = 1;

INSERT INTO company (
  company_name, company_type, contact_name, contact_phone, contact_email, status
)
SELECT '平台管理企业', 'PLATFORM_ADMIN', '平台管理员', '13800000000', 'admin@example.com', 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_user WHERE username = 'admin'
);

INSERT INTO sys_user (
  company_id, username, phone, password_hash, user_type, status
)
SELECT company.id, 'admin', '13800000000',
       'sha256$ZGV2LWFkbWluLXNlZWQtMQ$6DTX_6Qxe6Lk1eYgm9zE3Bp6YSBbPUOaAdaaeYXXDI4',
       'PLATFORM_ADMIN', 'ACTIVE'
FROM company
WHERE company.company_name = '平台管理企业'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'admin'
  )
ORDER BY company.id
LIMIT 1;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user.id, role.id
FROM sys_user user
JOIN sys_role role ON role.role_code = 'PLATFORM_ADMIN'
WHERE user.username = 'admin';
