CREATE TABLE IF NOT EXISTS company (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '企业ID',
  company_name VARCHAR(200) NOT NULL COMMENT '企业名称',
  company_type VARCHAR(50) NOT NULL COMMENT '企业类型',
  contact_name VARCHAR(100) NOT NULL COMMENT '联系人',
  contact_phone VARCHAR(50) NOT NULL COMMENT '联系电话',
  contact_email VARCHAR(160) NULL COMMENT '联系邮箱',
  status VARCHAR(30) NOT NULL DEFAULT 'PROFILE_REQUIRED' COMMENT '状态',
  review_comment VARCHAR(500) NULL COMMENT '审核意见',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_company_type (company_type),
  KEY idx_company_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业信息';

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  company_id BIGINT NOT NULL COMMENT '企业ID',
  username VARCHAR(100) NOT NULL COMMENT '登录账号',
  phone VARCHAR(50) NULL COMMENT '手机号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  user_type VARCHAR(50) NOT NULL COMMENT '用户类型',
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  is_company_owner TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否企业主账号',
  created_by BIGINT NULL COMMENT '创建人用户ID',
  invitation_status VARCHAR(30) NULL COMMENT '邀请状态',
  disabled_at DATETIME NULL COMMENT '禁用时间',
  last_password_reset_at DATETIME NULL COMMENT '最近重置密码时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_phone (phone),
  KEY idx_sys_user_company (company_id),
  KEY idx_sys_user_type (user_type),
  KEY idx_sys_user_owner (is_company_owner),
  KEY idx_sys_user_created_by (created_by),
  CONSTRAINT fk_sys_user_company
    FOREIGN KEY (company_id) REFERENCES company (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号';

CREATE TABLE IF NOT EXISTS sys_auth_token (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'TokenID',
  token VARCHAR(160) NOT NULL COMMENT '访问令牌',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  expires_at DATETIME NOT NULL COMMENT '过期时间',
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  last_used_at DATETIME NULL COMMENT '最后使用时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_auth_token_token (token),
  KEY idx_sys_auth_token_user (user_id),
  KEY idx_sys_auth_token_status_expiry (status, expires_at),
  CONSTRAINT fk_sys_auth_token_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证访问令牌';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(80) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
  role_type VARCHAR(50) NOT NULL COMMENT '角色类型',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  permission_code VARCHAR(120) NOT NULL COMMENT '权限编码',
  permission_name VARCHAR(160) NOT NULL COMMENT '权限名称',
  permission_type VARCHAR(40) NOT NULL COMMENT '权限类型：MENU/BUTTON/ACTION',
  resource_code VARCHAR(120) NULL COMMENT '资源编码',
  action_code VARCHAR(80) NULL COMMENT '动作编码',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_permission_code (permission_code),
  KEY idx_sys_permission_type (permission_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限点';

CREATE TABLE IF NOT EXISTS sys_role_permission (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  KEY idx_role_permission_permission (permission_id),
  CONSTRAINT fk_role_permission_role
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
  CONSTRAINT fk_role_permission_permission
    FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关系';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_user_role_role (role_id),
  CONSTRAINT fk_user_role_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_role_role
    FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  menu_code VARCHAR(120) NOT NULL COMMENT '菜单编码',
  menu_name VARCHAR(160) NOT NULL COMMENT '菜单名称',
  route_path VARCHAR(200) NOT NULL COMMENT '前端路由',
  icon VARCHAR(80) NULL COMMENT '图标',
  parent_code VARCHAR(120) NULL COMMENT '父菜单编码',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  required_permission VARCHAR(120) NULL COMMENT '所需权限编码',
  visible_roles VARCHAR(500) NULL COMMENT '可见角色编码，逗号分隔',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_menu_code (menu_code),
  KEY idx_sys_menu_parent (parent_code),
  KEY idx_sys_menu_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统菜单';

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  operator_user_id BIGINT NULL COMMENT '操作人用户ID',
  operation_type VARCHAR(80) NOT NULL COMMENT '操作类型',
  target_type VARCHAR(80) NULL COMMENT '目标类型',
  target_id VARCHAR(120) NULL COMMENT '目标ID',
  request_path VARCHAR(200) NULL COMMENT '请求路径',
  detail TEXT NULL COMMENT '操作详情',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_operation_log_operator (operator_user_id),
  KEY idx_operation_log_type (operation_type),
  KEY idx_operation_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志';

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

INSERT INTO sys_role (role_code, role_name, role_type, enabled)
VALUES
  ('SHIP_AGENT', '船代', 'SHIP_AGENT', 1),
  ('SUPPLIER', '供货商', 'SUPPLIER', 1),
  ('BARGE_AGENT', '驳船代理', 'BARGE_AGENT', 1),
  ('PLATFORM_ADMIN', '平台管理员', 'PLATFORM_ADMIN', 1)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  role_type = VALUES(role_type),
  enabled = VALUES(enabled);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, resource_code, action_code, enabled)
VALUES
  ('DASHBOARD_VIEW', 'Dashboard查看', 'MENU', 'DASHBOARD', 'VIEW', 1),
  ('PROCUREMENT_MATERIALS_VIEW', '物料需求查看', 'MENU', 'PROCUREMENT_MATERIALS', 'VIEW', 1),
  ('PROCUREMENT_FOOD_VIEW', '伙食需求查看', 'MENU', 'PROCUREMENT_FOOD', 'VIEW', 1),
  ('STANDARD_LIBRARY_IMPA_VIEW', 'IMPA标准库查看', 'MENU', 'STANDARD_LIBRARY_IMPA', 'VIEW', 1),
  ('SUPPLIER_VIEW', '供货商查看', 'MENU', 'SUPPLIER', 'VIEW', 1),
  ('QUOTE_VIEW', '报价查看', 'MENU', 'QUOTE', 'VIEW', 1),
  ('COMPARISON_VIEW', '比价查看', 'MENU', 'COMPARISON', 'VIEW', 1),
  ('ORDER_VIEW', '订单查看', 'MENU', 'ORDER', 'VIEW', 1),
  ('DELIVERY_TASK_VIEW', '送船任务查看', 'MENU', 'DELIVERY_TASK', 'VIEW', 1),
  ('SETTLEMENT_VIEW', '费用归档查看', 'MENU', 'SETTLEMENT', 'VIEW', 1),
  ('ADMIN_PERMISSION_VIEW', '权限管理查看', 'MENU', 'ADMIN_PERMISSION', 'VIEW', 1),
  ('ONBOARDING_PROFILE_VIEW', '入驻资料查看', 'MENU', 'ONBOARDING_PROFILE', 'VIEW', 1),
  ('ONBOARDING_REVIEW_STATUS_VIEW', '入驻审核状态查看', 'MENU', 'ONBOARDING_REVIEW_STATUS', 'VIEW', 1),
  ('ADMIN_REGISTRATION_REVIEW', '入驻审核管理', 'MENU', 'ADMIN_REGISTRATION', 'REVIEW', 1),
  ('FILE_UPLOAD', '文件上传', 'ACTION', 'FILE', 'UPLOAD', 1),
  ('AUTH_REGISTER', '注册', 'ACTION', 'AUTH', 'REGISTER', 1),
  ('AUTH_LOGIN', '登录', 'ACTION', 'AUTH', 'LOGIN', 1),
  ('USER_READ', '用户读取', 'ACTION', 'USER', 'READ', 1),
  ('ROLE_READ', '角色读取', 'ACTION', 'ROLE', 'READ', 1),
  ('MENU_READ', '菜单读取', 'ACTION', 'MENU', 'READ', 1),
  ('PERMISSION_READ', '权限读取', 'ACTION', 'PERMISSION', 'READ', 1),
  ('ROLE_PERMISSION_UPDATE', '角色权限更新', 'BUTTON', 'ROLE_PERMISSION', 'UPDATE', 1),
  ('USER_ROLE_UPDATE', '用户角色更新', 'BUTTON', 'USER_ROLE', 'UPDATE', 1)
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
  ('DASHBOARD', 'Dashboard', '/dashboard', 'LayoutDashboard', NULL, 0, 'DASHBOARD_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('PROCUREMENT_MATERIALS', '物料需求', '/procurement/materials', 'PackageSearch', NULL, 10, 'PROCUREMENT_MATERIALS_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('PROCUREMENT_FOOD', '伙食需求', '/procurement/food', 'Utensils', NULL, 20, 'PROCUREMENT_FOOD_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('STANDARD_LIBRARY_IMPA', 'IMPA标准库', '/standard-library/impa', 'Library', NULL, 30, 'STANDARD_LIBRARY_IMPA_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('SUPPLIERS', '供货商', '/suppliers', 'Building2', NULL, 40, 'SUPPLIER_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('QUOTES', '报价', '/quotes', 'FileText', NULL, 50, 'QUOTE_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('COMPARISON', '比价', '/comparison', 'Scale', NULL, 60, 'COMPARISON_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('ORDERS', '下单', '/orders', 'ShoppingCart', NULL, 70, 'ORDER_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('DELIVERY_TASKS', '送船任务', '/delivery-tasks', 'Ship', NULL, 80, 'DELIVERY_TASK_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('SETTLEMENT', '费用归档', '/settlement', 'Archive', NULL, 90, 'SETTLEMENT_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1),
  ('ONBOARDING_PROFILE', '企业入驻资料', '/onboarding/company-profile', 'ClipboardList', NULL, 800, 'ONBOARDING_PROFILE_VIEW', 'SHIP_AGENT,SUPPLIER', 1),
  ('ONBOARDING_REVIEW_STATUS', '入驻审核状态', '/onboarding/review-status', 'Hourglass', NULL, 810, 'ONBOARDING_REVIEW_STATUS_VIEW', 'SHIP_AGENT,SUPPLIER', 1),
  ('ADMIN_REGISTRATIONS', '入驻审核', '/admin/registrations', 'UserCheck', NULL, 890, 'ADMIN_REGISTRATION_REVIEW', 'PLATFORM_ADMIN', 1),
  ('ADMIN_PERMISSION', '权限管理', '/admin/permissions', 'ShieldCheck', NULL, 900, 'ADMIN_PERMISSION_VIEW', 'SHIP_AGENT,SUPPLIER,BARGE_AGENT,PLATFORM_ADMIN', 1)
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
