ALTER TABLE food_purchase_order
  ADD COLUMN required_delivery_time DATETIME NULL AFTER traffic_service_json,
  ADD COLUMN delivery_contact_name VARCHAR(255) NULL AFTER required_delivery_time,
  ADD COLUMN delivery_contact_phone VARCHAR(80) NULL AFTER delivery_contact_name,
  ADD COLUMN delivery_contact_email VARCHAR(255) NULL AFTER delivery_contact_phone,
  ADD COLUMN default_packaging_method VARCHAR(80) NULL AFTER delivery_contact_email,
  ADD COLUMN buyer_remark VARCHAR(2000) NULL AFTER default_packaging_method;
