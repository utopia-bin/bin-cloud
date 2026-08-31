-- 增量补齐系统配置表；不修改已应用的 V1/V2，不覆盖已有业务数据。
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    comment VARCHAR(500) NOT NULL DEFAULT '',
    sort INT NOT NULL DEFAULT 10,
    available TINYINT(1) NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    active_code VARCHAR(100) GENERATED ALWAYS AS
        (CASE WHEN is_delete = 0 THEN code ELSE NULL END) STORED,
    active_name VARCHAR(100) GENERATED ALWAYS AS
        (CASE WHEN is_delete = 0 THEN name ELSE NULL END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_tenant_id (tenant_id, id),
    UNIQUE KEY uk_dict_tenant_code (tenant_id, active_code),
    UNIQUE KEY uk_dict_tenant_name (tenant_id, active_name),
    KEY idx_dict_tenant_status (tenant_id, available, is_delete),
    CONSTRAINT fk_dict_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_dict_options (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    dict_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    option_name VARCHAR(100) NOT NULL,
    option_value VARCHAR(200) NOT NULL,
    option_comment VARCHAR(500) NOT NULL DEFAULT '',
    sort INT NOT NULL DEFAULT 10,
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    active_name VARCHAR(100) GENERATED ALWAYS AS
        (CASE WHEN is_delete = 0 THEN option_name ELSE NULL END) STORED,
    active_value VARCHAR(200) GENERATED ALWAYS AS
        (CASE WHEN is_delete = 0 THEN option_value ELSE NULL END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_option_tenant_name (tenant_id, dict_id, active_name),
    UNIQUE KEY uk_dict_option_tenant_value (tenant_id, dict_id, active_value),
    KEY idx_dict_option_parent (tenant_id, dict_id, parent_id, sort),
    CONSTRAINT fk_dict_option_dict FOREIGN KEY (tenant_id, dict_id)
        REFERENCES sys_dict(tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_parameter (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    param_key VARCHAR(100) NOT NULL,
    param_value TEXT NOT NULL,
    param_comment VARCHAR(500) NOT NULL DEFAULT '',
    sort INT NOT NULL DEFAULT 10,
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    active_key VARCHAR(100) GENERATED ALWAYS AS
        (CASE WHEN is_delete = 0 THEN param_key ELSE NULL END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_parameter_tenant_key (tenant_id, active_key),
    KEY idx_parameter_tenant_sort (tenant_id, is_delete, sort),
    CONSTRAINT fk_parameter_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 只补权限目录；不启用被禁用的权限、不修改已有角色授权。
-- 现有通配权限持有者无需额外授权；普通角色须显式分配。
INSERT IGNORE INTO sys_permission
    (id, tenant_id, name, code, description, available, sort, version, is_delete)
VALUES
    (701, NULL, '字典查看', 'platform:dict:read', '包含字典项和字典树查询', 1, 70, 0, 0),
    (702, NULL, '字典创建', 'platform:dict:create', '包含字典项创建', 1, 70, 0, 0),
    (703, NULL, '字典修改', 'platform:dict:update', '包含字典项修改和缓存刷新', 1, 70, 0, 0),
    (704, NULL, '字典删除', 'platform:dict:delete', '包含字典项删除', 1, 70, 0, 0),
    (801, NULL, '参数查看', 'platform:parameter:read', '', 1, 80, 0, 0),
    (802, NULL, '参数创建', 'platform:parameter:create', '', 1, 80, 0, 0),
    (803, NULL, '参数修改', 'platform:parameter:update', '包含缓存刷新', 1, 80, 0, 0),
    (804, NULL, '参数删除', 'platform:parameter:delete', '', 1, 80, 0, 0);
