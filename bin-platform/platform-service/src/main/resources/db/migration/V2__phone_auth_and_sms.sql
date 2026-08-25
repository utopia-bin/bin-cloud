ALTER TABLE sys_user
    ADD COLUMN active_phone VARCHAR(20) GENERATED ALWAYS AS
        (CASE WHEN is_delete = 0 AND phone <> '' THEN phone ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_user_tenant_phone (tenant_id, active_phone);

CREATE TABLE IF NOT EXISTS sys_sms_send_log (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    scene VARCHAR(32) NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT '',
    template_code VARCHAR(100) NOT NULL DEFAULT '',
    success TINYINT(1) NOT NULL DEFAULT 0,
    request_id VARCHAR(100) NULL,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(500) NULL,
    send_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    KEY idx_sms_log_tenant_phone_time (tenant_id, phone, send_time),
    KEY idx_sms_log_result_time (success, send_time),
    CONSTRAINT fk_sms_log_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
