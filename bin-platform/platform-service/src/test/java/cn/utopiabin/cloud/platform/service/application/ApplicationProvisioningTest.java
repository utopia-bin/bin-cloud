package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.model.dto.application.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationProvisioningTest {
    ApplicationFixture f;
    @BeforeEach void setup() throws Exception { f = new ApplicationFixture(); }
    @AfterEach void clear() { UserContextHolder.clear(); }
    @Test void provisionCreatesBoundedAdministratorAndRejectsDuplicate() {
        long id=f.provision(10,100);
        assertThat(f.boundary.access(10,100,id).get("status")).isEqualTo("ACTIVE");
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_permission WHERE tenant_application_id=?",Integer.class,id)).isEqualTo(2);
        assertThatThrownBy(()->f.provision(10,100)).isInstanceOf(BizException.class).hasMessageContaining("已存在");
        assertThatThrownBy(()->f.boundary.access(10,200,id)).isInstanceOf(BizException.class);
        assertThatThrownBy(()->f.boundary.access(20,200,id)).isInstanceOf(BizException.class);
    }
    @Test void rollbackLeavesNoPartialInstanceOrRole() {
        f.jdbc.execute("ALTER TABLE sys_user_application ADD CONSTRAINT reject_grant CHECK (user_id < 0)");
        assertThatThrownBy(()->f.provision(10,100)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM sys_tenant_application WHERE application_id=2",Integer.class)).isZero();
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM sys_role",Integer.class)).isZero();
    }
    @Test void assignedAndAllPolicyHaveDistinctEntrySemantics() {
        long id=f.provision(10,100);
        assertThatThrownBy(()->f.boundary.access(10,101,id)).isInstanceOf(BizException.class);
        f.jdbc.update("UPDATE sys_tenant_application SET access_policy='ALL' WHERE id=?",id);
        assertThat(f.boundary.access(10,101,id).get("user_id")).isEqualTo(101L);
        f.jdbc.update("UPDATE sys_user SET available=0 WHERE id=101");
        assertThatThrownBy(()->f.boundary.access(10,101,id)).isInstanceOf(BizException.class);
    }
    @Test void timeWindowsAndProductStateAreCheckedAtEveryEntry() {
        long id=f.provision(10,100);
        f.jdbc.update("UPDATE sys_user_application SET expire_at=? WHERE tenant_application_id=?",LocalDateTime.now().minusSeconds(1),id);
        assertThatThrownBy(()->f.boundary.access(10,100,id)).isInstanceOf(BizException.class);
        f.jdbc.update("UPDATE sys_user_application SET expire_at=NULL WHERE tenant_application_id=?",id);
        f.jdbc.update("UPDATE sys_application SET status='DISABLED' WHERE id=2");
        assertThatThrownBy(()->f.boundary.access(10,100,id)).isInstanceOf(BizException.class);
    }
    @Test void sameRoleCodeMayExistAcrossTenantsButCannotBeAssignedAcrossInstances() {
        long a=f.provision(10,100),b=f.provision(20,200);
        long foreignRole=f.jdbc.queryForObject("SELECT id FROM sys_role WHERE tenant_application_id=?",Long.class,b);
        var dto=new UserGrantDTO();dto.setTenantApplicationId(a);dto.setUserId(101L);dto.setRoleIds(List.of(foreignRole));
        assertThatThrownBy(()->f.transaction.executeWithoutResult(s->new ApplicationRbacService(f.store,f.boundary,f.revocations).grant(dto))).isInstanceOf(BizException.class);
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_role WHERE user_id=101",Integer.class)).isZero();
        assertThatThrownBy(()->f.jdbc.update("INSERT INTO sys_role_permission(id,tenant_id,application_id,tenant_application_id,role_id,permission_id) VALUES(555,20,2,?,?,1)",b,foreignRole)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void tenantAdminCannotReadAnotherTenantsMembers() {
        long other=f.provision(20,200);
        when(f.permissions.hasPermission(100L,"platform:application:provision")).thenReturn(false);
        assertThatThrownBy(()->new ApplicationRbacService(f.store,f.boundary,f.revocations).members(other)).isInstanceOf(BizException.class).hasMessageContaining("其他租户");
    }
    @Test void optimisticVersionAndRestoreReuseInstance() {
        long id=f.provision(10,100);
        var dto=new InstanceDTO();dto.setId(id);dto.setTenantId(10L);dto.setApplicationId(2L);dto.setExpectedVersion(0);dto.setStatus("CLOSED");
        var service=new TenantApplicationService(f.store,f.boundary,f.revocations);
        f.transaction.executeWithoutResult(s->service.save(dto));
        assertThatThrownBy(()->f.transaction.executeWithoutResult(s->service.save(dto))).isInstanceOf(BizException.class).hasMessageContaining("数据已变化");
        dto.setExpectedVersion(1);dto.setStatus("ACTIVE");
        Long restored = f.transaction.execute(s->service.save(dto));
        assertThat(restored).isEqualTo(id);
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM sys_tenant_application WHERE application_id=2",Integer.class)).isEqualTo(1);
    }
    @Test void resourceRoleGrantAndPaginationWorkTogether() {
        long id=f.provision(10,100);
        var rbac=new ApplicationRbacService(f.store,f.boundary,f.revocations);
        var permission=new ApplicationResourceDTO();permission.setApplicationId(2L);permission.setCode("workbench:extra");permission.setName("Extra");
        long permissionId=f.transaction.execute(tx->rbac.saveResource("permissions",permission));
        assertThat(rbac.resources(2,"permissions")).hasSize(3);
        var role=new ApplicationRoleDTO();role.setTenantApplicationId(id);role.setName("Reader");role.setCode("reader");role.setDataScope(4);role.setPermissionIds(List.of(permissionId));
        long roleId=f.transaction.execute(tx->rbac.saveRole(role));
        var grant=new UserGrantDTO();grant.setTenantApplicationId(id);grant.setUserId(101L);grant.setRoleIds(List.of(roleId));
        f.transaction.executeWithoutResult(tx->rbac.grant(grant));
        assertThat(f.boundary.access(10,101,id)).isNotEmpty();
        assertThat(rbac.members(id).stream().filter(u->u.getUserId()==101L).findFirst().orElseThrow().getRoleIds()).containsExactly(roleId);
        grant.setExpectedVersion(0);grant.setStatus("DISABLED");
        f.transaction.executeWithoutResult(tx->rbac.grant(grant));
        assertThatThrownBy(()->f.boundary.access(10,101,id)).isInstanceOf(BizException.class);
        var query=new ApplicationQuery();query.setSize(1);
        var catalog=new ApplicationCatalogService(f.store,f.boundary,f.revocations);
        assertThat(catalog.page(query).getRecords()).hasSize(1);
        assertThat(catalog.page(query).getTotal()).isEqualTo(2);
        assertThat(catalog.get(2).getRedirectUris()).hasSize(1);
    }
    @Test void menuRejectsForeignParentAndCycle() {
        var rbac=new ApplicationRbacService(f.store,f.boundary,f.revocations);
        var menu=new ApplicationResourceDTO();menu.setApplicationId(2L);menu.setName("Extra");menu.setPath("/extra");menu.setParentId(901L);
        long id=f.transaction.execute(tx->rbac.saveResource("menus",menu));
        menu.setId(901L);menu.setExpectedVersion(0);menu.setParentId(id);
        assertThatThrownBy(()->f.transaction.execute(tx->rbac.saveResource("menus",menu))).isInstanceOf(BizException.class).hasMessageContaining("循环");
        f.jdbc.update("INSERT INTO sys_menu(id,application_id,name) VALUES(1,1,'Console')");
        menu.setId(null);menu.setParentId(1L);
        assertThatThrownBy(()->f.transaction.execute(tx->rbac.saveResource("menus",menu))).isInstanceOf(BizException.class);
    }
    @Test void productionRedirectRejectsHttpButLocalDevAcceptsIt() {
        assertThat(SsoCrypto.redirect("http://localhost:5173/api/open/workbench/callback","DEV")).startsWith("http:");
        for(String uri:List.of("http://localhost/callback","https://example.com/callback?next=x","https://example.com/callback#x","https://u@example.com/callback","https://example.com/a/../callback"))
            assertThatThrownBy(()->SsoCrypto.redirect(uri,"PROD")).isInstanceOf(BizException.class);
    }
}
