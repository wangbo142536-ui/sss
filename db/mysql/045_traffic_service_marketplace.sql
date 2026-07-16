CREATE TABLE IF NOT EXISTS traffic_service_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_no VARCHAR(64) NOT NULL UNIQUE,
  requester_company_id BIGINT NOT NULL,
  demand_id BIGINT NULL,
  purchase_order_id BIGINT NULL,
  fee_type VARCHAR(32) DEFAULT 'FREIGHT',
  sea_area VARCHAR(64) NOT NULL,
  anchorage_code VARCHAR(64) NOT NULL,
  anchorage_name VARCHAR(128) NOT NULL,
  use_time DATETIME NULL,
  service_type VARCHAR(32) DEFAULT 'GOODS',
  passenger_type VARCHAR(32) DEFAULT 'NORMAL',
  passenger_count INT NULL,
  cargo_type VARCHAR(32) DEFAULT 'CARGO',
  return_trip TINYINT(1) DEFAULT 0,
  allow_share TINYINT(1) DEFAULT 0,
  remark VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  recommended_quote_id BIGINT NULL,
  selected_quote_id BIGINT NULL,
  selected_supplier_company_id BIGINT NULL,
  traffic_service_order_id BIGINT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_traffic_service_request_requester (requester_company_id, status),
  INDEX idx_traffic_service_request_anchor (sea_area, anchorage_code),
  INDEX idx_traffic_service_request_order (traffic_service_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS traffic_service_request_cargo (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  cargo_name VARCHAR(255) NOT NULL,
  weight_kg DECIMAL(12,2) NULL,
  volume_cbm DECIMAL(12,3) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_traffic_service_request_cargo_request (request_id),
  CONSTRAINT fk_traffic_request_cargo_request
    FOREIGN KEY (request_id) REFERENCES traffic_service_request(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS traffic_service_quote (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  supplier_company_id BIGINT NOT NULL,
  supplier_company_name VARCHAR(255) NULL,
  quote_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  base_price DECIMAL(12,2) NULL,
  shared_price DECIMAL(12,2) NULL,
  currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
  available_start_time DATETIME NULL,
  available_return_time DATETIME NULL,
  traffic_vessel_id BIGINT NULL,
  traffic_vessel_name VARCHAR(128) NULL,
  contact_name VARCHAR(128) NULL,
  contact_phone VARCHAR(64) NULL,
  message VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_traffic_service_quote_supplier (request_id, supplier_company_id),
  INDEX idx_traffic_service_quote_request_status (request_id, status),
  INDEX idx_traffic_service_quote_supplier_status (supplier_company_id, status),
  CONSTRAINT fk_traffic_service_quote_request
    FOREIGN KEY (request_id) REFERENCES traffic_service_request(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS traffic_service_request_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  quote_id BIGINT NULL,
  event_type VARCHAR(64) NOT NULL,
  event_message VARCHAR(1000) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_traffic_service_request_event_request (request_id, created_at),
  CONSTRAINT fk_traffic_request_event_request
    FOREIGN KEY (request_id) REFERENCES traffic_service_request(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS traffic_shuttle_service (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shuttle_no VARCHAR(64) NOT NULL UNIQUE,
  supplier_company_id BIGINT NOT NULL,
  supplier_company_name VARCHAR(255) NULL,
  sea_area VARCHAR(64) NOT NULL,
  anchorage_code VARCHAR(64) NOT NULL,
  anchorage_name VARCHAR(128) NOT NULL,
  departure_point VARCHAR(128) NULL,
  destination_point VARCHAR(128) NULL,
  start_time DATETIME NOT NULL,
  return_time DATETIME NULL,
  shared_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  customs_price DECIMAL(12,2) NULL,
  crane_price DECIMAL(12,2) NULL,
  passenger_capacity INT NULL,
  cargo_capacity_kg DECIMAL(12,2) NULL,
  cargo_capacity_cbm DECIMAL(12,3) NULL,
  booked_passenger_count INT NOT NULL DEFAULT 0,
  booked_cargo_weight_kg DECIMAL(12,2) NOT NULL DEFAULT 0,
  booked_cargo_volume_cbm DECIMAL(12,3) NOT NULL DEFAULT 0,
  traffic_vessel_id BIGINT NULL,
  traffic_vessel_name VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  remark VARCHAR(1000) NULL,
  service_nodes_json JSON NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_traffic_shuttle_supplier (supplier_company_id, status),
  INDEX idx_traffic_shuttle_anchor (sea_area, anchorage_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS traffic_shuttle_booking (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_no VARCHAR(64) NOT NULL UNIQUE,
  shuttle_service_id BIGINT NOT NULL,
  requester_company_id BIGINT NOT NULL,
  request_id BIGINT NULL,
  request_no VARCHAR(80) NULL,
  traffic_service_order_id BIGINT NULL,
  passenger_count INT NULL,
  cargo_summary VARCHAR(1000) NULL,
  cargo_weight_kg DECIMAL(12,2) NULL,
  cargo_volume_cbm DECIMAL(12,3) NULL,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  allow_share TINYINT(1) NOT NULL DEFAULT 1,
  customs_service TINYINT(1) NOT NULL DEFAULT 0,
  crane_service TINYINT(1) NOT NULL DEFAULT 0,
  crane_count INT NOT NULL DEFAULT 0,
  freight_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
  customs_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
  crane_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
  contact_name VARCHAR(128) NULL,
  contact_phone VARCHAR(64) NULL,
  remark VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'BOOKED',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_traffic_shuttle_booking_shuttle (shuttle_service_id, status),
  INDEX idx_traffic_shuttle_booking_requester (requester_company_id, status),
  CONSTRAINT fk_traffic_shuttle_booking_shuttle
    FOREIGN KEY (shuttle_service_id) REFERENCES traffic_shuttle_service(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
