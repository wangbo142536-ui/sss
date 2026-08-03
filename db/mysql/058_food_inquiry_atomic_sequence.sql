CREATE TABLE IF NOT EXISTS food_inquiry_daily_sequence (
  buyer_company_id BIGINT NOT NULL,
  inquiry_date DATE NOT NULL,
  last_sequence INT NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (buyer_company_id, inquiry_date),
  CONSTRAINT fk_food_inquiry_sequence_company FOREIGN KEY (buyer_company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO food_inquiry_daily_sequence (buyer_company_id, inquiry_date, last_sequence)
SELECT buyer_company_id,
       STR_TO_DATE(SUBSTRING(inquiry_no, 5, 8), '%Y%m%d'),
       MAX(CAST(SUBSTRING(inquiry_no, 13) AS UNSIGNED))
FROM food_demand
WHERE inquiry_no REGEXP '^FOOD[0-9]{11}$'
GROUP BY buyer_company_id, STR_TO_DATE(SUBSTRING(inquiry_no, 5, 8), '%Y%m%d')
ON DUPLICATE KEY UPDATE last_sequence = GREATEST(last_sequence, VALUES(last_sequence));

SET @has_unique_inquiry_no := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'food_demand'
    AND index_name = 'uk_food_demand_company_inquiry_no'
);
SET @add_unique_inquiry_no_sql := IF(
  @has_unique_inquiry_no = 0,
  'ALTER TABLE food_demand ADD UNIQUE KEY uk_food_demand_company_inquiry_no (buyer_company_id, inquiry_no)',
  'SELECT ''food_demand inquiry unique key already exists'''
);
PREPARE add_unique_inquiry_no_stmt FROM @add_unique_inquiry_no_sql;
EXECUTE add_unique_inquiry_no_stmt;
DEALLOCATE PREPARE add_unique_inquiry_no_stmt;
