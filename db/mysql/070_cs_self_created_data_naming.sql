-- Normalize known self-created test, demonstration, and acceptance business data.
-- The retired marker is encoded so new source-controlled business data cannot reintroduce it.

START TRANSACTION;

SET @retired_lower = CONVERT(0x636F646578 USING utf8mb4);
SET @retired_title = CONVERT(0x436F646578 USING utf8mb4);
SET @retired_upper = CONVERT(0x434F444558 USING utf8mb4);
SET @current_prefix = 'cs';

UPDATE sys_user AS source
LEFT JOIN sys_user AS conflicting
    ON conflicting.username = 'cs_supplier_0817_qa'
   AND conflicting.id <> source.id
SET source.username = 'cs_supplier_0817_qa'
WHERE source.id = 34
  AND source.username = CONCAT(@retired_lower, '_supplier_0817_qa')
  AND conflicting.id IS NULL;

UPDATE sys_user AS source
LEFT JOIN sys_user AS conflicting
    ON conflicting.username = 'cs_member_0817_qa'
   AND conflicting.id <> source.id
SET source.username = 'cs_member_0817_qa',
    source.email = REPLACE(source.email, @retired_lower, @current_prefix)
WHERE source.id = 35
  AND source.username = CONCAT(@retired_lower, '_member_0817_qa')
  AND conflicting.id IS NULL;

UPDATE company
SET company_name = REPLACE(company_name, @retired_title, @current_prefix),
    contact_email = REPLACE(contact_email, @retired_lower, @current_prefix)
WHERE id = 35;

UPDATE shop_store
SET shop_name = REPLACE(shop_name, @retired_title, @current_prefix)
WHERE id = 11
  AND company_id = 35;

UPDATE company_contact
SET contact_name = REPLACE(REPLACE(REPLACE(contact_name, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    contact_email = REPLACE(REPLACE(REPLACE(contact_email, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix)
WHERE id IN (1, 2, 3);

UPDATE company_qualification
SET description = REPLACE(REPLACE(REPLACE(description, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix)
WHERE id = 11;

UPDATE material_demand
SET application_no = REPLACE(REPLACE(REPLACE(application_no, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    inquiry_no = REPLACE(REPLACE(REPLACE(inquiry_no, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    traffic_service_json = REPLACE(REPLACE(REPLACE(traffic_service_json, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    recipient_company = REPLACE(REPLACE(REPLACE(recipient_company, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    handler_name = REPLACE(REPLACE(REPLACE(handler_name, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    handler_email = REPLACE(REPLACE(REPLACE(handler_email, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    vessel_name = REPLACE(REPLACE(REPLACE(vessel_name, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix)
WHERE id IN (31, 32, 33);

UPDATE purchase_order
SET application_no = REPLACE(REPLACE(REPLACE(application_no, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    vessel_name = REPLACE(REPLACE(REPLACE(vessel_name, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix),
    buyer_remark = REPLACE(REPLACE(REPLACE(buyer_remark, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix)
WHERE id IN (15, 17, 18);

UPDATE traffic_service_order
SET remark = REPLACE(REPLACE(REPLACE(remark, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix)
WHERE id IN (1, 2);

UPDATE operation_log
SET detail = REPLACE(REPLACE(REPLACE(detail, @retired_lower, @current_prefix), @retired_title, @current_prefix), @retired_upper, @current_prefix)
WHERE id = 978;

COMMIT;
