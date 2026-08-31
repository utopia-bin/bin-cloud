-- Run before V4/V5. Every query should return zero rows. Read-only; do not suppress failures.
SELECT 'permission reserved id conflict' AS problem,id FROM sys_permission WHERE id IN (801,802,803,804,805,806,807,901,902);
SELECT 'menu reserved id conflict' AS problem,id FROM sys_menu WHERE id IN (801,802,803,901);
SELECT 'role without tenant' AS problem,r.id FROM sys_role r LEFT JOIN sys_tenant t ON t.id=r.tenant_id WHERE t.id IS NULL;
SELECT 'user role boundary' AS problem,ur.id FROM sys_user_role ur LEFT JOIN sys_user u ON u.id=ur.user_id AND u.tenant_id=ur.tenant_id LEFT JOIN sys_role r ON r.id=ur.role_id AND r.tenant_id=ur.tenant_id WHERE u.id IS NULL OR r.id IS NULL;
SELECT 'role permission boundary' AS problem,rp.id FROM sys_role_permission rp LEFT JOIN sys_role r ON r.id=rp.role_id AND r.tenant_id=rp.tenant_id LEFT JOIN sys_permission p ON p.id=rp.permission_id WHERE r.id IS NULL OR p.id IS NULL;
SELECT 'missing menu parent' AS problem,m.id FROM sys_menu m LEFT JOIN sys_menu p ON p.id=m.parent_id WHERE m.parent_id<>0 AND p.id IS NULL;
