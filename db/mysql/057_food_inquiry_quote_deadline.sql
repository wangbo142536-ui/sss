ALTER TABLE food_inquiry_supplier
  ADD COLUMN IF NOT EXISTS quote_deadline_at DATETIME NULL AFTER sent_at;

UPDATE food_inquiry_supplier
SET quote_deadline_at = DATE_ADD(sent_at, INTERVAL 3 DAY)
WHERE quote_deadline_at IS NULL;
