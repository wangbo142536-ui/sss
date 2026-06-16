SET @phone_is_nullable := (
  SELECT is_nullable
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'phone'
);

SET @modify_phone_sql := IF(
  @phone_is_nullable = 'NO',
  'ALTER TABLE sys_user MODIFY COLUMN phone VARCHAR(50) NULL COMMENT ''手机号''',
  'SELECT ''sys_user.phone already nullable'''
);

PREPARE modify_phone_stmt FROM @modify_phone_sql;
EXECUTE modify_phone_stmt;
DEALLOCATE PREPARE modify_phone_stmt;
