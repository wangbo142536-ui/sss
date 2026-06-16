USE ship_supply_platform;

-- Enterprise management profile and qualifications.
-- Idempotent: only adds missing columns and updates menu display text.
-- It does not clear roles, permissions, users, SKU data, or qualification data.

SET @schema_name := DATABASE();

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE company ADD COLUMN logo_file_id VARCHAR(80) NULL COMMENT ''公司LOGO文件ID'' AFTER unified_social_credit_code',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'company' AND COLUMN_NAME = 'logo_file_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE company ADD COLUMN logo_url VARCHAR(500) NULL COMMENT ''公司LOGO地址'' AFTER logo_file_id',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'company' AND COLUMN_NAME = 'logo_url'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE company_qualification ADD COLUMN title VARCHAR(200) NULL COMMENT ''资质标题'' AFTER file_name',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'company_qualification' AND COLUMN_NAME = 'title'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE company_qualification ADD COLUMN description VARCHAR(1000) NULL COMMENT ''资质说明'' AFTER title',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'company_qualification' AND COLUMN_NAME = 'description'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE company_qualification ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER created_at',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'company_qualification' AND COLUMN_NAME = 'updated_at'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_permission
SET permission_name = '企业管理查看',
    resource_code = 'SHOP_MANAGEMENT',
    action_code = 'VIEW',
    enabled = 1
WHERE permission_code = 'SHOP_MANAGEMENT_VIEW';

UPDATE sys_menu
SET menu_name = '企业管理',
    route_path = '/shop/products',
    icon = 'Building2',
    enabled = 1
WHERE menu_code = 'SHOP_MANAGEMENT';
