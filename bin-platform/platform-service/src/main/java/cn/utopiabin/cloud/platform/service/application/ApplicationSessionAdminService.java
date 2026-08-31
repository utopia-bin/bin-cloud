package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.vo.application.*;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.*;

@Service
@RequiredArgsConstructor
public class ApplicationSessionAdminService {
    private final ApplicationStore store;
    private final ApplicationBoundary boundary;
    private final SsoAuditService audit;

    public PageResult<SsoSessionVO> sessions(ApplicationQuery q) {
        boundary.require("audit"); var args=new ArrayList<Object>();
        String from="FROM sys_sso_session s JOIN sys_application a ON a.id=s.application_id JOIN sys_user u ON u.id=s.user_id AND u.tenant_id=s.tenant_id WHERE s.is_delete=0";
        if(q.getTenantId()!=null || !boundary.global()) {from+=" AND s.tenant_id=?";args.add(boundary.queryTenant(q.getTenantId()));}
        if(q.getApplicationId()!=null) {from+=" AND s.application_id=?";args.add(q.getApplicationId());}
        if(q.getTenantApplicationId()!=null) {from+=" AND s.tenant_application_id=?";args.add(q.getTenantApplicationId());}
        if(q.getStatus()!=null && !q.getStatus().isBlank()) {from+=" AND (CASE WHEN s.status='ACTIVE' AND s.expire_at<=CURRENT_TIMESTAMP THEN 'EXPIRED' ELSE s.status END)=?";args.add(q.getStatus());}
        return store.page(SsoSessionVO.class,q,from,"s.session_id,s.parent_session_id,s.tenant_id,s.application_id,s.tenant_application_id,s.user_id,s.auth_time,s.expire_at,s.revoked_at,s.revoke_reason,a.name AS application_name,u.username,CASE WHEN s.status='ACTIVE' AND s.expire_at<=CURRENT_TIMESTAMP THEN 'EXPIRED' ELSE s.status END AS status","s.auth_time DESC,s.id DESC",args);
    }

    public PageResult<SsoAuditVO> logs(ApplicationQuery q) {
        boundary.require("audit"); var args=new ArrayList<Object>(); String from="FROM sys_sso_login_log WHERE 1=1";
        if(q.getTenantId()!=null || !boundary.global()) {from+=" AND tenant_id=?";args.add(boundary.queryTenant(q.getTenantId()));}
        if(q.getApplicationId()!=null) {from+=" AND application_id=?";args.add(q.getApplicationId());}
        if(q.getTenantApplicationId()!=null) {from+=" AND tenant_application_id=?";args.add(q.getTenantApplicationId());}
        if("SUCCESS".equals(q.getStatus()) || "FAILURE".equals(q.getStatus())) {from+=" AND success=?";args.add("SUCCESS".equals(q.getStatus()));}
        return store.page(SsoAuditVO.class,q,from,"id,tenant_id,application_id,tenant_application_id,user_id,event_type,success,failure_code,session_id,trace_id,event_time","event_time DESC,id DESC",args);
    }

    @Transactional
    @OperateLog(module="应用会话",action="强制下线",type=OperateType.AUTH,maskParams=true)
    public void revoke(String sid) {
        boundary.require("revoke");
        var session=store.one("SELECT * FROM sys_sso_session WHERE session_id=? AND is_delete=0 FOR UPDATE",sid);
        long tenant=number(session,"tenant_id");boundary.queryTenant(tenant);
        store.jdbc().update("UPDATE sys_sso_session SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP,revoke_reason='ADMIN_REVOKED',version=version+1 WHERE tenant_id=? AND (session_id=? OR parent_session_id=?) AND status='ACTIVE'",tenant,sid,sid);
        TransactionAfterCommitExecutor.afterCommit(()->audit.record("REVOKED",true,"ADMIN_REVOKED",tenant,number(session,"application_id"),number(session,"tenant_application_id"),number(session,"user_id"),sid));
    }
}
