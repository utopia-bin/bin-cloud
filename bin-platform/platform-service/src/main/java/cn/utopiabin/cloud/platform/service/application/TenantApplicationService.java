package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.model.dto.application.*;
import cn.utopiabin.cloud.platform.model.vo.application.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.*;

@Service
@RequiredArgsConstructor
public class TenantApplicationService {
    private final ApplicationStore store;
    private final ApplicationBoundary boundary;
    private final ApplicationRevocationService revocations;
    private static final String SELECT = "ta.*,a.code AS application_code,a.name AS application_name,a.icon_url,t.name AS tenant_name,COALESCE(NULLIF(ta.entry_url_override,''),a.entry_url) AS entry_url," +
            "CASE WHEN ta.status<>'ACTIVE' THEN ta.status WHEN a.status<>'ENABLED' OR a.is_delete<>0 OR t.available<>1 OR t.is_delete<>0 THEN 'UNAVAILABLE' " +
            "WHEN ta.expire_at<=CURRENT_TIMESTAMP OR t.expire_time<=CURRENT_TIMESTAMP THEN 'EXPIRED' WHEN ta.effective_at>CURRENT_TIMESTAMP THEN 'PENDING' ELSE 'ACTIVE' END AS effective_status";
    private static final String FROM = "FROM sys_tenant_application ta JOIN sys_application a ON a.id=ta.application_id JOIN sys_tenant t ON t.id=ta.tenant_id WHERE ta.is_delete=0";

    public PageResult<TenantApplicationVO> page(ApplicationQuery q) {
        boundary.require("read");
        var args=new ArrayList<Object>(); String from=FROM;
        if (q.getTenantId()!=null || !boundary.global()) { from+=" AND ta.tenant_id=?"; args.add(boundary.queryTenant(q.getTenantId())); }
        if (q.getApplicationId()!=null) { from+=" AND ta.application_id=?"; args.add(q.getApplicationId()); }
        if (q.getStatus()!=null && !q.getStatus().isBlank()) { from+=" AND ta.status=?";args.add(q.getStatus()); }
        if (q.getKeyword()!=null && !q.getKeyword().isBlank()) { from+=" AND (a.name LIKE ? OR t.name LIKE ?)";args.add("%"+q.getKeyword()+"%");args.add("%"+q.getKeyword()+"%"); }
        return store.page(TenantApplicationVO.class,q,from,SELECT,"ta.id DESC",args);
    }

    public List<TenantApplicationVO> mine() {
        long tenant=boundary.tenantId(),user=boundary.userId();
        var list=store.list(TenantApplicationVO.class,"SELECT "+SELECT+" "+FROM+" AND ta.tenant_id=? ORDER BY a.sort,ta.id",tenant);
        return list.stream().filter(item->{
            try { boundary.access(tenant,user,item.getId()); return true; }
            catch (BizException e) { return false; }
        }).toList();
    }

    public static void ensureConsole(JdbcTemplate jdbc,long tenant) {
        if (jdbc.queryForList("SELECT id FROM sys_tenant_application WHERE tenant_id=? AND application_id=1 AND is_delete=0",tenant).isEmpty())
            jdbc.update("INSERT INTO sys_tenant_application (id,tenant_id,application_id,status,access_policy,opened_at) VALUES (?,?,1,'ACTIVE','ALL',CURRENT_TIMESTAMP)",tenant,tenant);
    }

    @Transactional
    @OperateLog(module="应用开通",action="开通或调整租户应用",type=OperateType.UPDATE,maskParams=true)
    public long save(InstanceDTO dto) {
        boundary.require("provision");
        if (dto.getApplicationId()==1) throw new BizException(400,"平台壳随租户自动开通，生命周期由租户管理控制");
        window(dto.getEffectiveAt(),dto.getExpireAt());
        SsoCrypto.navigation(dto.getEntryUrlOverride(),true);
        // Product first, then tenant: use the same lock order as app status changes.
        var app=store.one("SELECT * FROM sys_application WHERE id=? AND is_delete=0 FOR UPDATE",dto.getApplicationId());
        var tenant=store.one("SELECT * FROM sys_tenant WHERE id=? AND is_delete=0 FOR UPDATE",dto.getTenantId());
        if ("ACTIVE".equals(dto.getStatus()) && (!"ENABLED".equals(app.get("status")) || !flag(tenant,"available") || !within(null,time(tenant,"expire_time"),LocalDateTime.now())))
            throw new BizException(400,"租户或应用不可用，不能开通或恢复");
        if (dto.getId()!=null) {
            var old=store.one("SELECT * FROM sys_tenant_application WHERE id=? AND tenant_id=? AND application_id=? AND is_delete=0 FOR UPDATE",dto.getId(),dto.getTenantId(),dto.getApplicationId());
            if (!List.of("ACTIVE","SUSPENDED","CLOSED","EXPIRED","PENDING").contains(old.get("status"))) throw new BizException(400,"不支持的原始状态");
            changed(store.jdbc().update("""
                    UPDATE sys_tenant_application SET status=?,access_policy=?,entry_url_override=?,effective_at=?,expire_at=?,comment=?,
                      suspended_at=CASE WHEN ?='SUSPENDED' THEN CURRENT_TIMESTAMP ELSE suspended_at END,
                      closed_at=CASE WHEN ?='CLOSED' THEN CURRENT_TIMESTAMP ELSE closed_at END,
                      version=version+1,modify_user=? WHERE id=? AND tenant_id=? AND application_id=? AND version=? AND is_delete=0
                    """,dto.getStatus(),dto.getAccessPolicy(),dto.getEntryUrlOverride(),dto.getEffectiveAt(),dto.getExpireAt(),dto.getComment(),dto.getStatus(),dto.getStatus(),String.valueOf(boundary.userId()),dto.getId(),dto.getTenantId(),dto.getApplicationId(),version(dto.getExpectedVersion())));
            revocations.instance(dto.getTenantId(),dto.getId(),"INSTANCE_CHANGED");
            return dto.getId();
        }
        if (!"ACTIVE".equals(dto.getStatus())) throw new BizException(400,"首次开通必须选择ACTIVE");
        if (!store.jdbc().queryForList("SELECT id FROM sys_tenant_application WHERE tenant_id=? AND application_id=? AND is_delete=0",dto.getTenantId(),dto.getApplicationId()).isEmpty())
            throw new BizException(409,"已存在开通实例，请编辑原实例恢复或续期");
        if (dto.getAdminUserId()==null) throw new BizException(400,"首次开通请选择租户内的应用管理员");
        store.one("SELECT id FROM sys_user WHERE id=? AND tenant_id=? AND available=1 AND is_delete=0",dto.getAdminUserId(),dto.getTenantId());
        long id=IdWorker.getId(),role=IdWorker.getId();
        store.jdbc().update("""
                INSERT INTO sys_tenant_application (id,tenant_id,application_id,status,access_policy,entry_url_override,opened_at,effective_at,expire_at,comment,create_user,modify_user)
                VALUES (?,?,?,'PENDING',?,?,CURRENT_TIMESTAMP,?,?,?,?,?)
                """,id,dto.getTenantId(),dto.getApplicationId(),dto.getAccessPolicy(),dto.getEntryUrlOverride(),dto.getEffectiveAt(),dto.getExpireAt(),dto.getComment(),String.valueOf(boundary.userId()),String.valueOf(boundary.userId()));
        store.jdbc().update("INSERT INTO sys_role (id,tenant_id,application_id,tenant_application_id,name,code,data_scope) VALUES (?,?,?,?,'应用管理员','app_admin',1)",role,dto.getTenantId(),dto.getApplicationId(),id);
        for (var permission:store.jdbc().queryForList("SELECT id FROM sys_permission WHERE application_id=? AND available=1 AND is_delete=0",dto.getApplicationId())) {
            store.jdbc().update("INSERT INTO sys_role_permission (id,tenant_id,application_id,tenant_application_id,role_id,permission_id) VALUES (?,?,?,?,?,?)",IdWorker.getId(),dto.getTenantId(),dto.getApplicationId(),id,role,permission.get("id"));
        }
        store.jdbc().update("INSERT INTO sys_user_role (id,tenant_id,application_id,tenant_application_id,user_id,role_id) VALUES (?,?,?,?,?,?)",IdWorker.getId(),dto.getTenantId(),dto.getApplicationId(),id,dto.getAdminUserId(),role);
        store.jdbc().update("INSERT INTO sys_user_application (id,tenant_id,tenant_application_id,user_id,granted_by) VALUES (?,?,?,?,?)",IdWorker.getId(),dto.getTenantId(),id,dto.getAdminUserId(),boundary.userId());
        store.jdbc().update("UPDATE sys_tenant_application SET status='ACTIVE' WHERE id=?",id);
        return id;
    }

    public List<UserApplicationVO> candidates(long tenant) {
        boundary.require("provision");
        return store.list(UserApplicationVO.class,"SELECT id AS user_id,username FROM sys_user WHERE tenant_id=? AND available=1 AND is_delete=0 ORDER BY username",tenant);
    }
}
