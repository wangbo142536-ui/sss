SET @food_evaluation_attachments_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'food_evaluation'
    AND column_name = 'attachments_json'
);

SET @food_evaluation_attachments_ddl = IF(
  @food_evaluation_attachments_column_exists = 0,
  'ALTER TABLE food_evaluation ADD COLUMN attachments_json JSON NULL AFTER comment',
  'SELECT 1'
);

PREPARE food_evaluation_attachments_statement FROM @food_evaluation_attachments_ddl;
EXECUTE food_evaluation_attachments_statement;
DEALLOCATE PREPARE food_evaluation_attachments_statement;
