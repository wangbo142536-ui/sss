-- Add unified social credit code for onboarding profile.
-- Idempotent: adds a nullable column without default value; onboarding service enforces required submission.

SET @has_unified_social_credit_code := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'company'
    AND column_name = 'unified_social_credit_code'
);

SET @add_unified_social_credit_code_sql := IF(
  @has_unified_social_credit_code = 0,
  'ALTER TABLE company ADD COLUMN unified_social_credit_code VARCHAR(64) NULL COMMENT ''统一社会信用代码'' AFTER company_type',
  'SELECT ''company.unified_social_credit_code already exists'''
);

PREPARE add_unified_social_credit_code_stmt FROM @add_unified_social_credit_code_sql;
EXECUTE add_unified_social_credit_code_stmt;
DEALLOCATE PREPARE add_unified_social_credit_code_stmt;

SET @has_unified_social_credit_code_index := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'company'
    AND index_name = 'idx_company_unified_social_credit_code'
);

SET @add_unified_social_credit_code_index_sql := IF(
  @has_unified_social_credit_code_index = 0,
  'ALTER TABLE company ADD INDEX idx_company_unified_social_credit_code (unified_social_credit_code)',
  'SELECT ''idx_company_unified_social_credit_code already exists'''
);

PREPARE add_unified_social_credit_code_index_stmt FROM @add_unified_social_credit_code_index_sql;
EXECUTE add_unified_social_credit_code_index_stmt;
DEALLOCATE PREPARE add_unified_social_credit_code_index_stmt;
