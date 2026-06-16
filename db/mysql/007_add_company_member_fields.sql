SET @has_is_company_owner := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'is_company_owner'
);
SET @add_is_company_owner_sql := IF(
  @has_is_company_owner = 0,
  'ALTER TABLE sys_user ADD COLUMN is_company_owner TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否企业主账号'' AFTER status',
  'SELECT ''sys_user.is_company_owner already exists'''
);
PREPARE add_is_company_owner_stmt FROM @add_is_company_owner_sql;
EXECUTE add_is_company_owner_stmt;
DEALLOCATE PREPARE add_is_company_owner_stmt;

SET @has_created_by := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'created_by'
);
SET @add_created_by_sql := IF(
  @has_created_by = 0,
  'ALTER TABLE sys_user ADD COLUMN created_by BIGINT NULL COMMENT ''创建人用户ID'' AFTER is_company_owner',
  'SELECT ''sys_user.created_by already exists'''
);
PREPARE add_created_by_stmt FROM @add_created_by_sql;
EXECUTE add_created_by_stmt;
DEALLOCATE PREPARE add_created_by_stmt;

SET @has_invitation_status := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'invitation_status'
);
SET @add_invitation_status_sql := IF(
  @has_invitation_status = 0,
  'ALTER TABLE sys_user ADD COLUMN invitation_status VARCHAR(30) NULL COMMENT ''邀请状态'' AFTER created_by',
  'SELECT ''sys_user.invitation_status already exists'''
);
PREPARE add_invitation_status_stmt FROM @add_invitation_status_sql;
EXECUTE add_invitation_status_stmt;
DEALLOCATE PREPARE add_invitation_status_stmt;

SET @has_disabled_at := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'disabled_at'
);
SET @add_disabled_at_sql := IF(
  @has_disabled_at = 0,
  'ALTER TABLE sys_user ADD COLUMN disabled_at DATETIME NULL COMMENT ''禁用时间'' AFTER invitation_status',
  'SELECT ''sys_user.disabled_at already exists'''
);
PREPARE add_disabled_at_stmt FROM @add_disabled_at_sql;
EXECUTE add_disabled_at_stmt;
DEALLOCATE PREPARE add_disabled_at_stmt;

SET @has_last_password_reset_at := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'last_password_reset_at'
);
SET @add_last_password_reset_at_sql := IF(
  @has_last_password_reset_at = 0,
  'ALTER TABLE sys_user ADD COLUMN last_password_reset_at DATETIME NULL COMMENT ''最近重置密码时间'' AFTER disabled_at',
  'SELECT ''sys_user.last_password_reset_at already exists'''
);
PREPARE add_last_password_reset_at_stmt FROM @add_last_password_reset_at_sql;
EXECUTE add_last_password_reset_at_stmt;
DEALLOCATE PREPARE add_last_password_reset_at_stmt;

SET @has_owner_index := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'idx_sys_user_owner'
);
SET @add_owner_index_sql := IF(
  @has_owner_index = 0,
  'ALTER TABLE sys_user ADD INDEX idx_sys_user_owner (is_company_owner)',
  'SELECT ''sys_user.idx_sys_user_owner already exists'''
);
PREPARE add_owner_index_stmt FROM @add_owner_index_sql;
EXECUTE add_owner_index_stmt;
DEALLOCATE PREPARE add_owner_index_stmt;

SET @has_created_by_index := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'idx_sys_user_created_by'
);
SET @add_created_by_index_sql := IF(
  @has_created_by_index = 0,
  'ALTER TABLE sys_user ADD INDEX idx_sys_user_created_by (created_by)',
  'SELECT ''sys_user.idx_sys_user_created_by already exists'''
);
PREPARE add_created_by_index_stmt FROM @add_created_by_index_sql;
EXECUTE add_created_by_index_stmt;
DEALLOCATE PREPARE add_created_by_index_stmt;

UPDATE sys_user user
JOIN (
  SELECT company_id, MIN(id) AS owner_user_id
  FROM sys_user
  GROUP BY company_id
) owner ON owner.owner_user_id = user.id
SET user.is_company_owner = 1,
    user.created_by = user.id
WHERE user.is_company_owner = 0;
