-- Ensure every supplier enterprise has a shop profile before setting the common service port.
-- Idempotent data migration: existing profile fields and non-supplier companies are preserved.

INSERT INTO shop_store (company_id, service_ports, status)
SELECT c.id, '舟山港', c.status
FROM company c
WHERE c.company_type = 'SUPPLIER'
ON DUPLICATE KEY UPDATE company_id = VALUES(company_id);

UPDATE shop_store s
JOIN company c ON c.id = s.company_id
SET s.service_ports = '舟山港'
WHERE c.company_type = 'SUPPLIER'
  AND COALESCE(s.service_ports, '') <> '舟山港';
