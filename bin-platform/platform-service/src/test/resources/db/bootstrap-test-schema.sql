-- H2 MySQL 模式测试夹具：只包含初始化所需字段；不替代生产 MySQL 的 V1/V2 迁移。
CREATE TABLE sys_tenant (
    id BIGINT PRIMARY KEY, name VARCHAR(100) NOT NULL, code VARCHAR(50) NOT NULL UNIQUE,
    available TINYINT DEFAULT 1 NOT NULL, is_delete TINYINT DEFAULT 0 NOT NULL, expire_time TIMESTAMP
);
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    username VARCHAR(50) NOT NULL, password VARCHAR(100) NOT NULL, real_name VARCHAR(50) NOT NULL,
    is_delete TINYINT DEFAULT 0 NOT NULL, UNIQUE (tenant_id, username), UNIQUE (tenant_id, id)
);
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    name VARCHAR(50) NOT NULL, code VARCHAR(50) NOT NULL, data_scope TINYINT NOT NULL,
    available TINYINT DEFAULT 1 NOT NULL, is_delete TINYINT DEFAULT 0 NOT NULL,
    UNIQUE (tenant_id, code), UNIQUE (tenant_id, id)
);
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE, description VARCHAR(200) NOT NULL,
    available TINYINT DEFAULT 1 NOT NULL, is_delete TINYINT DEFAULT 0 NOT NULL
);
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL,
    UNIQUE (tenant_id, user_id, role_id),
    FOREIGN KEY (tenant_id, user_id) REFERENCES sys_user(tenant_id, id),
    FOREIGN KEY (tenant_id, role_id) REFERENCES sys_role(tenant_id, id)
);
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL REFERENCES sys_permission(id),
    UNIQUE (tenant_id, role_id, permission_id),
    FOREIGN KEY (tenant_id, role_id) REFERENCES sys_role(tenant_id, id)
);
CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY, type TINYINT NOT NULL, name VARCHAR(50) NOT NULL,
    path VARCHAR(200) NOT NULL, icon VARCHAR(100) NOT NULL,
    permission VARCHAR(100) NOT NULL, sort INT NOT NULL
);

CREATE TABLE sys_tenant_application(id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, application_id BIGINT NOT NULL, status VARCHAR(16), access_policy VARCHAR(16), opened_at TIMESTAMP, is_delete INT DEFAULT 0, UNIQUE(tenant_id,application_id));
ALTER TABLE sys_role ADD application_id BIGINT DEFAULT 1;
ALTER TABLE sys_permission ADD application_id BIGINT DEFAULT 1;
ALTER TABLE sys_menu ADD application_id BIGINT DEFAULT 1;
ALTER TABLE sys_user_role ADD application_id BIGINT DEFAULT 1;
ALTER TABLE sys_role_permission ADD application_id BIGINT DEFAULT 1;
ALTER TABLE sys_role ADD tenant_application_id BIGINT;
ALTER TABLE sys_user_role ADD tenant_application_id BIGINT;
ALTER TABLE sys_role_permission ADD tenant_application_id BIGINT;
