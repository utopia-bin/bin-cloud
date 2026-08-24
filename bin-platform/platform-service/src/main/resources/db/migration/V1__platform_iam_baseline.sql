-- 学习项目采用全新基线，不保留旧 sys_role_menu 模型。
CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    contact_name VARCHAR(50) NOT NULL DEFAULT '',
    contact_phone VARCHAR(20) NOT NULL DEFAULT '',
    contact_email VARCHAR(100) NOT NULL DEFAULT '',
    expire_time DATETIME NULL,
    available TINYINT(1) NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 10,
    comment VARCHAR(500) NOT NULL DEFAULT '',
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL DEFAULT '',
    phone VARCHAR(20) NOT NULL DEFAULT '',
    email VARCHAR(100) NOT NULL DEFAULT '',
    gender TINYINT NOT NULL DEFAULT 0,
    available TINYINT(1) NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 10,
    comment VARCHAR(500) NOT NULL DEFAULT '',
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    active_username VARCHAR(50) GENERATED ALWAYS AS
        (CASE WHEN is_delete = 0 THEN username ELSE NULL END) STORED,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tenant_id (tenant_id, id),
    UNIQUE KEY uk_user_tenant_username (tenant_id, active_username),
    KEY idx_user_tenant_status (tenant_id, available, is_delete),
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    data_scope TINYINT NOT NULL DEFAULT 4,
    available TINYINT(1) NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 10,
    comment VARCHAR(500) NOT NULL DEFAULT '',
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    active_code VARCHAR(50) GENERATED ALWAYS AS
        (CASE WHEN is_delete = 0 THEN code ELSE NULL END) STORED,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_tenant_id (tenant_id, id),
    UNIQUE KEY uk_role_tenant_code (tenant_id, active_code),
    KEY idx_role_tenant_status (tenant_id, available, is_delete),
    CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(200) NOT NULL DEFAULT '',
    available TINYINT(1) NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 10,
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    type TINYINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(200) NOT NULL DEFAULT '',
    component VARCHAR(200) NOT NULL DEFAULT '',
    icon VARCHAR(100) NOT NULL DEFAULT '',
    permission VARCHAR(100) NOT NULL DEFAULT '',
    sort INT NOT NULL DEFAULT 10,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    available TINYINT(1) NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    KEY idx_menu_parent (parent_id, sort),
    KEY idx_menu_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role_tenant (tenant_id, user_id, role_id),
    KEY idx_user_role_role (tenant_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (tenant_id, user_id)
        REFERENCES sys_user(tenant_id, id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (tenant_id, role_id)
        REFERENCES sys_role(tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission_tenant (tenant_id, role_id, permission_id),
    KEY idx_role_permission_permission (tenant_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (tenant_id, role_id)
        REFERENCES sys_role(tenant_id, id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id),
    CONSTRAINT fk_role_permission_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_operate_log (
    id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    module VARCHAR(100) NOT NULL DEFAULT '',
    action VARCHAR(100) NOT NULL DEFAULT '',
    type VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    method VARCHAR(200) NOT NULL DEFAULT '',
    params VARCHAR(1000) NOT NULL DEFAULT '',
    success TINYINT(1) NOT NULL DEFAULT 1,
    error_msg VARCHAR(1024) NULL,
    cost_ms BIGINT NOT NULL DEFAULT 0,
    operate_user_id VARCHAR(64) NULL,
    operate_username VARCHAR(50) NULL,
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    is_delete TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modify DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_user VARCHAR(64) NOT NULL DEFAULT 'system',
    modify_user VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    KEY idx_operate_log_tenant_time (tenant_id, operate_time),
    KEY idx_operate_log_operator (operate_username, operate_time),
    KEY idx_operate_log_result (success, operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO sys_permission
    (id, tenant_id, name, code, description, available, sort, version, is_delete)
VALUES
    (1, NULL, '全部权限', '*', '仅供平台内置超级管理员使用', 1, 0, 0, 0),
    (101, NULL, '租户查看', 'platform:tenant:read', '', 1, 10, 0, 0),
    (102, NULL, '租户创建', 'platform:tenant:create', '', 1, 10, 0, 0),
    (103, NULL, '租户修改', 'platform:tenant:update', '', 1, 10, 0, 0),
    (104, NULL, '租户删除', 'platform:tenant:delete', '', 1, 10, 0, 0),
    (201, NULL, '用户查看', 'platform:user:read', '', 1, 20, 0, 0),
    (202, NULL, '用户创建', 'platform:user:create', '', 1, 20, 0, 0),
    (203, NULL, '用户修改', 'platform:user:update', '', 1, 20, 0, 0),
    (204, NULL, '用户删除', 'platform:user:delete', '', 1, 20, 0, 0),
    (205, NULL, '用户分配角色', 'platform:user:assign-role', '', 1, 20, 0, 0),
    (206, NULL, '用户重置密码', 'platform:user:reset-password', '', 1, 20, 0, 0),
    (301, NULL, '角色查看', 'platform:role:read', '', 1, 30, 0, 0),
    (302, NULL, '角色创建', 'platform:role:create', '', 1, 30, 0, 0),
    (303, NULL, '角色修改', 'platform:role:update', '', 1, 30, 0, 0),
    (304, NULL, '角色删除', 'platform:role:delete', '', 1, 30, 0, 0),
    (305, NULL, '角色分配权限', 'platform:role:assign-permission', '', 1, 30, 0, 0),
    (401, NULL, '菜单查看', 'platform:menu:read', '', 1, 40, 0, 0),
    (402, NULL, '菜单创建', 'platform:menu:create', '', 1, 40, 0, 0),
    (403, NULL, '菜单修改', 'platform:menu:update', '', 1, 40, 0, 0),
    (404, NULL, '菜单删除', 'platform:menu:delete', '', 1, 40, 0, 0),
    (501, NULL, '权限查看', 'platform:permission:read', '', 1, 50, 0, 0),
    (502, NULL, '权限创建', 'platform:permission:create', '', 1, 50, 0, 0),
    (503, NULL, '权限修改', 'platform:permission:update', '', 1, 50, 0, 0),
    (504, NULL, '权限删除', 'platform:permission:delete', '', 1, 50, 0, 0),
    (601, NULL, '操作日志查看', 'platform:operate-log:read', '', 1, 60, 0, 0);

-- 新库初始化超级管理员角色后，可通过初始化服务绑定通配权限；此处不假设固定角色 ID。
