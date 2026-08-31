-- 已有身份不复制、不改密码；每个租户默认开通平台壳。
INSERT INTO sys_tenant_application (id, tenant_id, application_id, status, access_policy, opened_at)
SELECT id, id, 1, 'ACTIVE', 'ALL', CURRENT_TIMESTAMP FROM sys_tenant;

ALTER TABLE sys_user ADD COLUMN credential_version INT NOT NULL DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN last_login_at DATETIME NULL;
ALTER TABLE sys_permission ADD COLUMN application_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_menu ADD COLUMN application_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_menu ADD COLUMN route_name VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE sys_menu ADD COLUMN open_mode VARCHAR(16) NOT NULL DEFAULT 'INTERNAL';
ALTER TABLE sys_role ADD COLUMN application_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_role ADD COLUMN tenant_application_id BIGINT NULL;
ALTER TABLE sys_user_role ADD COLUMN application_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_user_role ADD COLUMN tenant_application_id BIGINT NULL;
ALTER TABLE sys_role_permission ADD COLUMN application_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_role_permission ADD COLUMN tenant_application_id BIGINT NULL;
UPDATE sys_role SET tenant_application_id = tenant_id;
UPDATE sys_user_role SET tenant_application_id = tenant_id;
UPDATE sys_role_permission SET tenant_application_id = tenant_id;
ALTER TABLE sys_role MODIFY tenant_application_id BIGINT NOT NULL;
ALTER TABLE sys_user_role MODIFY tenant_application_id BIGINT NOT NULL;
ALTER TABLE sys_role_permission MODIFY tenant_application_id BIGINT NOT NULL;

ALTER TABLE sys_permission DROP INDEX uk_permission_code,
    ADD UNIQUE KEY uk_permission_application_code (application_id, code),
    ADD UNIQUE KEY uk_permission_application_id (application_id, id),
    ADD CONSTRAINT fk_permission_application FOREIGN KEY (application_id) REFERENCES sys_application(id);
ALTER TABLE sys_menu ADD KEY idx_menu_application_parent (application_id, parent_id, sort),
    ADD KEY idx_menu_application_permission (application_id, permission),
    ADD CONSTRAINT fk_menu_application FOREIGN KEY (application_id) REFERENCES sys_application(id);
ALTER TABLE sys_role DROP INDEX uk_role_tenant_code,
    ADD UNIQUE KEY uk_role_application_code (tenant_application_id, active_code),
    ADD UNIQUE KEY uk_role_application_boundary (tenant_id, application_id, tenant_application_id, id),
    ADD CONSTRAINT fk_role_application_boundary FOREIGN KEY (tenant_id, application_id, tenant_application_id)
        REFERENCES sys_tenant_application(tenant_id, application_id, id);
ALTER TABLE sys_user_role DROP INDEX uk_user_role_tenant,
    ADD UNIQUE KEY uk_user_role_application (tenant_application_id, user_id, role_id),
    ADD CONSTRAINT fk_user_role_application_boundary FOREIGN KEY (tenant_id, application_id, tenant_application_id, role_id)
        REFERENCES sys_role(tenant_id, application_id, tenant_application_id, id);
ALTER TABLE sys_role_permission DROP INDEX uk_role_permission_tenant,
    ADD UNIQUE KEY uk_role_permission_application (tenant_application_id, role_id, permission_id),
    ADD CONSTRAINT fk_role_permission_application_boundary FOREIGN KEY (tenant_id, application_id, tenant_application_id, role_id)
        REFERENCES sys_role(tenant_id, application_id, tenant_application_id, id),
    ADD CONSTRAINT fk_role_permission_product FOREIGN KEY (application_id, permission_id)
        REFERENCES sys_permission(application_id, id);
ALTER TABLE sys_operate_log ADD COLUMN application_id BIGINT NULL,
    ADD COLUMN tenant_application_id BIGINT NULL,
    ADD COLUMN trace_id VARCHAR(64) NOT NULL DEFAULT '',
    ADD COLUMN session_id VARCHAR(64) NOT NULL DEFAULT '';

INSERT INTO sys_permission (id, application_id, name, code) VALUES
    (801, 1, '应用目录查看', 'platform:application:read'),
    (802, 1, '应用目录维护', 'platform:application:manage'),
    (803, 1, '租户应用开通', 'platform:application:provision'),
    (804, 1, '本租户应用授权', 'platform:application:grant'),
    (805, 1, '本租户应用角色管理', 'platform:application:role'),
    (806, 1, '应用会话与审计', 'platform:application:audit'),
    (807, 1, '应用会话撤销', 'platform:application:revoke'),
    (901, 2, '工作台查看', 'workbench:read'),
    (902, 2, '示例功能执行', 'workbench:execute');

-- 本租户管理员只增加本租户的管理能力，不增加全局应用目录写或开通权限。
INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, application_id, tenant_application_id)
SELECT existing.max_id + ROW_NUMBER() OVER (ORDER BY r.id, p.id), r.tenant_id, r.id, p.id, 1, r.tenant_application_id
FROM sys_role r JOIN sys_permission p ON p.id IN (801, 804, 805, 806, 807)
CROSS JOIN (SELECT COALESCE(MAX(id), 0) AS max_id FROM sys_role_permission) existing
WHERE r.code = 'tenant_admin' AND r.application_id = 1 AND r.is_delete = 0;

INSERT INTO sys_menu (id, application_id, type, name, path, component, icon, permission, sort) VALUES
    (801, 1, 2, '我的应用', '/applications/mine', 'applications/mine/index', 'Grid', '', 5),
    (802, 1, 2, '应用管理', '/applications/manage', 'applications/manage/index', 'Connection', 'platform:application:read', 85),
    (803, 1, 2, '应用会话与审计', '/applications/sessions', 'applications/sessions/index', 'Monitor', 'platform:application:audit', 86),
    (901, 2, 2, '工作台概览', '/applications/workbench', 'applications/workbench/index', 'Odometer', 'workbench:read', 10);
