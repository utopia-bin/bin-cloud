package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.model.dto.application.*;
import cn.utopiabin.cloud.platform.model.vo.application.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.*;

@Service
@RequiredArgsConstructor
public class ApplicationRbacService {
    private final ApplicationStore store;
    private final ApplicationBoundary boundary;
    private final ApplicationRevocationService revocations;

    private void nonConsole(long app) { if (app==1) throw new BizException(400,"平台壳的用户和角色请在原IAM页面维护"); }

    public List<UserApplicationVO> members(long instance) {
        var scope=boundary.instance(instance,"read"); long tenant=number(scope,"tenant_id");
        var rows=store.list(UserApplicationVO.class,"""
                SELECT ua.id,ua.version,u.id AS user_id,? AS tenant_application_id,u.username,
                       COALESCE(ua.status,'UNASSIGNED') AS status,ua.effective_at,ua.expire_at,ua.comment
                FROM sys_user u LEFT JOIN sys_user_application ua ON ua.tenant_id=u.tenant_id AND ua.user_id=u.id
                    AND ua.tenant_application_id=? AND ua.is_delete=0
                WHERE u.tenant_id=? AND u.is_delete=0 ORDER BY u.username
                """,instance,instance,tenant);
        rows.forEach(row->row.setRoleIds(store.jdbc().queryForList("SELECT role_id FROM sys_user_role WHERE tenant_id=? AND tenant_application_id=? AND user_id=?",Long.class,tenant,instance,row.getUserId())));
        return rows;
    }

    @Transactional
    @OperateLog(module="应用授权",action="设置应用成员与角色",type=OperateType.UPDATE,maskParams=true)
    public void grant(UserGrantDTO dto) {
        var scope=boundary.instance(dto.getTenantApplicationId(),"grant");
        long tenant=number(scope,"tenant_id"),app=number(scope,"application_id"),instance=dto.getTenantApplicationId(); nonConsole(app);
        store.one("SELECT id FROM sys_tenant_application WHERE id=? AND tenant_id=? FOR UPDATE",instance,tenant);
        store.one("SELECT id FROM sys_user WHERE id=? AND tenant_id=? AND is_delete=0",dto.getUserId(),tenant);
        window(dto.getEffectiveAt(),dto.getExpireAt());
        var roleIds=dto.getRoleIds().stream().distinct().toList();
        for (var id:roleIds) store.one("SELECT id FROM sys_role WHERE id=? AND tenant_id=? AND application_id=? AND tenant_application_id=? AND available=1 AND is_delete=0",id,tenant,app,instance);
        var existing=store.jdbc().queryForList("SELECT * FROM sys_user_application WHERE tenant_id=? AND tenant_application_id=? AND user_id=? AND is_delete=0 FOR UPDATE",tenant,instance,dto.getUserId());
        if (existing.isEmpty()) {
            if (dto.getExpectedVersion()!=null) throw new BizException(409,"授权记录已变化，请刷新");
            store.jdbc().update("INSERT INTO sys_user_application (id,tenant_id,tenant_application_id,user_id,status,effective_at,expire_at,granted_by,comment) VALUES (?,?,?,?,?,?,?,?,?)",
                    IdWorker.getId(),tenant,instance,dto.getUserId(),dto.getStatus(),dto.getEffectiveAt(),dto.getExpireAt(),boundary.userId(),dto.getComment());
        } else changed(store.jdbc().update("UPDATE sys_user_application SET status=?,effective_at=?,expire_at=?,comment=?,granted_by=?,granted_at=CURRENT_TIMESTAMP,version=version+1 WHERE id=? AND version=?",
                dto.getStatus(),dto.getEffectiveAt(),dto.getExpireAt(),dto.getComment(),boundary.userId(),existing.getFirst().get("id"),version(dto.getExpectedVersion())));
        store.jdbc().update("DELETE FROM sys_user_role WHERE tenant_id=? AND tenant_application_id=? AND user_id=?",tenant,instance,dto.getUserId());
        for (var id:roleIds) store.jdbc().update("INSERT INTO sys_user_role (id,tenant_id,application_id,tenant_application_id,user_id,role_id) VALUES (?,?,?,?,?,?)",IdWorker.getId(),tenant,app,instance,dto.getUserId(),id);
        revocations.member(tenant,instance,dto.getUserId(),"MEMBERSHIP_CHANGED");
    }

    public List<ApplicationRoleVO> roles(long instance) {
        var scope=boundary.instance(instance,"read"); long tenant=number(scope,"tenant_id");
        var rows=store.list(ApplicationRoleVO.class,"SELECT * FROM sys_role WHERE tenant_id=? AND tenant_application_id=? AND is_delete=0 ORDER BY sort,id",tenant,instance);
        rows.forEach(row->row.setPermissionIds(store.jdbc().queryForList("SELECT permission_id FROM sys_role_permission WHERE tenant_id=? AND tenant_application_id=? AND role_id=?",Long.class,tenant,instance,row.getId())));
        return rows;
    }

    @Transactional
    @OperateLog(module="应用角色",action="保存应用角色与权限",type=OperateType.UPDATE,maskParams=true)
    public long saveRole(ApplicationRoleDTO dto) {
        var scope=boundary.instance(dto.getTenantApplicationId(),"role");
        long tenant=number(scope,"tenant_id"),app=number(scope,"application_id"),instance=dto.getTenantApplicationId(); nonConsole(app);
        store.one("SELECT id FROM sys_tenant_application WHERE id=? AND tenant_id=? FOR UPDATE",instance,tenant);
        if (dto.getDataScope()!=1 && dto.getDataScope()!=4) throw new BizException(400,"仅支持全部或本人数据范围");
        for (var permission:dto.getPermissionIds().stream().distinct().toList())
            store.one("SELECT id FROM sys_permission WHERE id=? AND application_id=? AND available=1 AND is_delete=0",permission,app);
        long id=dto.getId()==null?IdWorker.getId():dto.getId();
        if (dto.getId()==null) store.jdbc().update("INSERT INTO sys_role (id,tenant_id,application_id,tenant_application_id,name,code,data_scope,available,sort) VALUES (?,?,?,?,?,?,?,?,?)",
                id,tenant,app,instance,dto.getName(),dto.getCode(),dto.getDataScope(),dto.isAvailable(),dto.getSort());
        else {
            var old=store.one("SELECT * FROM sys_role WHERE id=? AND tenant_id=? AND application_id=? AND tenant_application_id=? AND is_delete=0",id,tenant,app,instance);
            if ("app_admin".equals(old.get("code")) && (!"app_admin".equals(dto.getCode()) || !dto.isAvailable())) throw new BizException(400,"内置应用管理员角色不能改编码或停用");
            changed(store.jdbc().update("UPDATE sys_role SET name=?,code=?,data_scope=?,available=?,sort=?,version=version+1 WHERE id=? AND tenant_id=? AND tenant_application_id=? AND version=? AND is_delete=0",
                    dto.getName(),dto.getCode(),dto.getDataScope(),dto.isAvailable(),dto.getSort(),id,tenant,instance,version(dto.getExpectedVersion())));
        }
        store.jdbc().update("DELETE FROM sys_role_permission WHERE tenant_id=? AND tenant_application_id=? AND role_id=?",tenant,instance,id);
        for (var permission:dto.getPermissionIds().stream().distinct().toList()) store.jdbc().update("INSERT INTO sys_role_permission (id,tenant_id,application_id,tenant_application_id,role_id,permission_id) VALUES (?,?,?,?,?,?)",IdWorker.getId(),tenant,app,instance,id,permission);
        revocations.instance(tenant,instance,"ROLE_CHANGED");
        return id;
    }

    @Transactional
    @OperateLog(module="应用角色",action="删除应用角色",type=OperateType.DELETE,maskParams=true)
    public void removeRole(long instance,long id,int version) {
        var scope=boundary.instance(instance,"role"); long tenant=number(scope,"tenant_id"); nonConsole(number(scope,"application_id"));
        store.one("SELECT id FROM sys_tenant_application WHERE id=? AND tenant_id=? FOR UPDATE",instance,tenant);
        var role=store.one("SELECT * FROM sys_role WHERE id=? AND tenant_id=? AND tenant_application_id=? AND is_delete=0",id,tenant,instance);
        if ("app_admin".equals(role.get("code"))) throw new BizException(400,"不能删除内置应用管理员角色");
        changed(store.jdbc().update("UPDATE sys_role SET is_delete=1,version=version+1 WHERE id=? AND tenant_id=? AND tenant_application_id=? AND version=? AND is_delete=0",id,tenant,instance,version));
        store.jdbc().update("DELETE FROM sys_user_role WHERE tenant_id=? AND tenant_application_id=? AND role_id=?",tenant,instance,id);
        store.jdbc().update("DELETE FROM sys_role_permission WHERE tenant_id=? AND tenant_application_id=? AND role_id=?",tenant,instance,id);
        revocations.instance(tenant,instance,"ROLE_DELETED");
    }

    private String table(String kind) {
        return switch(kind) { case "permissions" -> "sys_permission"; case "menus" -> "sys_menu"; default -> throw new BizException(400,"未知资源类型"); };
    }
    public List<ApplicationResourceVO> resources(long app,String kind) {
        boundary.require("read");
        return store.list(ApplicationResourceVO.class,"SELECT * FROM "+table(kind)+" WHERE application_id=? AND is_delete=0 ORDER BY sort,id",app);
    }

    @Transactional
    @OperateLog(module="应用资源",action="保存应用权限或菜单",type=OperateType.UPDATE,maskParams=true)
    public long saveResource(String kind,ApplicationResourceDTO dto) {
        boundary.require("manage"); nonConsole(dto.getApplicationId());
        String table=table(kind); long app=dto.getApplicationId();
        store.one("SELECT id FROM sys_application WHERE id=? AND is_delete=0 FOR UPDATE",app);
        long id=dto.getId()==null?IdWorker.getId():dto.getId();
        Map<String,Object> old=dto.getId()==null?null:store.one("SELECT * FROM "+table+" WHERE id=? AND application_id=? AND is_delete=0",id,app);
        if ("permissions".equals(kind)) {
            if (!dto.getCode().matches("[A-Za-z][A-Za-z0-9:_.*-]{0,99}")) throw new BizException(400,"请输入合法的应用权限编码，不允许全局通配符");
            if (old!=null && !dto.getCode().equals(old.get("code"))) throw new BizException(400,"已发布的权限编码不可修改，请新建权限并重新授权");
            if (old==null) store.jdbc().update("INSERT INTO sys_permission (id,application_id,name,code,description,available,sort) VALUES (?,?,?,?,?,?,?)",id,app,dto.getName(),dto.getCode(),dto.getDescription(),dto.isAvailable(),dto.getSort());
            else changed(store.jdbc().update("UPDATE sys_permission SET name=?,description=?,available=?,sort=?,version=version+1 WHERE id=? AND application_id=? AND version=? AND is_delete=0",dto.getName(),dto.getDescription(),dto.isAvailable(),dto.getSort(),id,app,version(dto.getExpectedVersion())));
        } else {
            long parent=dto.getParentId()==null?0:dto.getParentId();
            var visited=new HashSet<Long>(); visited.add(id);
            for(long cursor=parent;cursor!=0;) {
                if (!visited.add(cursor)) throw new BizException(400,"菜单父子关系不能形成循环");
                var node=store.one("SELECT parent_id,type FROM sys_menu WHERE id=? AND application_id=? AND is_delete=0",cursor,app);
                if (number(node,"type")==3) throw new BizException(400,"按钮不能包含子菜单");
                cursor=number(node,"parent_id");
            }
            if (!dto.getPermission().isBlank()) store.one("SELECT id FROM sys_permission WHERE code=? AND application_id=? AND is_delete=0",dto.getPermission(),app);
            if (dto.getType()==2 && !dto.getPath().matches("/[A-Za-z0-9_/-]+")) throw new BizException(400,"页面菜单必须填写应用内绝对路径");
            if (dto.getType()==3 && !store.jdbc().queryForList("SELECT id FROM sys_menu WHERE parent_id=? AND application_id=? AND is_delete=0",id,app).isEmpty()) throw new BizException(400,"含下级的菜单不能改为按钮");
            if(old==null) store.jdbc().update("INSERT INTO sys_menu (id,application_id,parent_id,type,name,path,component,icon,permission,route_name,open_mode,visible,available,sort) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,app,parent,dto.getType(),dto.getName(),dto.getPath(),dto.getComponent(),dto.getIcon(),dto.getPermission(),dto.getRouteName(),dto.getOpenMode(),dto.isVisible(),dto.isAvailable(),dto.getSort());
            else changed(store.jdbc().update("UPDATE sys_menu SET parent_id=?,type=?,name=?,path=?,component=?,icon=?,permission=?,route_name=?,open_mode=?,visible=?,available=?,sort=?,version=version+1 WHERE id=? AND application_id=? AND version=? AND is_delete=0",parent,dto.getType(),dto.getName(),dto.getPath(),dto.getComponent(),dto.getIcon(),dto.getPermission(),dto.getRouteName(),dto.getOpenMode(),dto.isVisible(),dto.isAvailable(),dto.getSort(),id,app,version(dto.getExpectedVersion())));
        }
        revocations.application(app,"RESOURCE_CHANGED"); return id;
    }

    @Transactional
    @OperateLog(module="应用资源",action="删除应用资源",type=OperateType.DELETE,maskParams=true)
    public void removeResource(long app,String kind,long id,int version) {
        boundary.require("manage"); nonConsole(app); String table=table(kind);
        store.one("SELECT id FROM sys_application WHERE id=? AND is_delete=0 FOR UPDATE",app);
        var old=store.one("SELECT * FROM "+table+" WHERE id=? AND application_id=? AND is_delete=0",id,app);
        if ("menus".equals(kind) && !store.jdbc().queryForList("SELECT id FROM sys_menu WHERE application_id=? AND parent_id=? AND is_delete=0",app,id).isEmpty()) throw new BizException(409,"请先删除下级菜单");
        if ("permissions".equals(kind) && (!store.jdbc().queryForList("SELECT id FROM sys_role_permission WHERE application_id=? AND permission_id=?",app,id).isEmpty()
                || !store.jdbc().queryForList("SELECT id FROM sys_menu WHERE application_id=? AND permission=? AND is_delete=0",app,old.get("code")).isEmpty())) throw new BizException(409,"权限仍被角色或菜单引用，请先解除关联");
        changed(store.jdbc().update("UPDATE "+table+" SET is_delete=1,version=version+1 WHERE id=? AND application_id=? AND version=? AND is_delete=0",id,app,version));
        revocations.application(app,"RESOURCE_DELETED");
    }
}
