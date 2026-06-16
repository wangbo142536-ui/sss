CREATE TABLE IF NOT EXISTS sys_auth_token (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'TokenID',
  token VARCHAR(160) NOT NULL COMMENT '访问令牌',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  expires_at DATETIME NOT NULL COMMENT '过期时间',
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  last_used_at DATETIME NULL COMMENT '最后使用时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_auth_token_token (token),
  KEY idx_sys_auth_token_user (user_id),
  KEY idx_sys_auth_token_status_expiry (status, expires_at),
  CONSTRAINT fk_sys_auth_token_user
    FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证访问令牌';
