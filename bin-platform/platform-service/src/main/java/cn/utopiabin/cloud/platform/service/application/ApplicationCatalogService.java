package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.model.dto.application.*;
import cn.utopiabin.cloud.platform.model.vo.application.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.*;

@Service
@RequiredArgsConstructor
public class ApplicationCatalogService {
    private final ApplicationStore store;
    private final ApplicationBoundary boundary;
    private final ApplicationRevocationService revocations;

    public PageResult<ApplicationVO> page(ApplicationQuery q) {
        boundary.require("read");
        var args = new ArrayList<Object>();
        String from = "FROM sys_application WHERE is_delete=0";
        if (q.getKeyword()!=null && !q.getKeyword().isBlank()) { from+=" AND (name LIKE ? OR code LIKE ?)"; args.add("%"+q.getKeyword()+"%"); args.add("%"+q.getKeyword()+"%"); }
        if (q.getStatus()!=null && !q.getStatus().isBlank()) { from+=" AND status=?"; args.add(q.getStatus()); }
        return store.page(ApplicationVO.class,q,from,"*, (client_secret_hash IS NOT NULL) AS client_configured","sort,id",args);
    }

    public ApplicationVO get(long id) {
        boundary.require("read");
        var result = store.list(ApplicationVO.class,"SELECT *, (client_secret_hash IS NOT NULL) AS client_configured FROM sys_application WHERE id=? AND is_delete=0",id);
        if (result.isEmpty()) throw new BizException(404,"应用不存在");
        var vo=result.getFirst();
        vo.setRedirectUris(store.list(RedirectDTO.class,"SELECT * FROM sys_application_redirect_uri WHERE application_id=? AND is_delete=0 ORDER BY id",id));
        return vo;
    }

    @Transactional
    @OperateLog(module="应用管理",action="保存应用产品",type=OperateType.UPDATE,maskParams=true)
    public long save(ApplicationDTO dto) {
        boundary.require("manage");
        SsoCrypto.navigation(dto.getEntryUrl(),false);
        SsoCrypto.navigation(dto.getIconUrl(),true);
        var seen=new HashSet<String>();
        for (var redirect:dto.getRedirectUris()) {
            SsoCrypto.redirect(redirect.getRedirectUri(),redirect.getEnvironment());
            if (!redirect.getLogoutUri().isBlank()) SsoCrypto.redirect(redirect.getLogoutUri(),redirect.getEnvironment());
            if (!seen.add(redirect.getRedirectUri())) throw new BizException(400,"回调地址不能重复");
        }
        if (dto.isSsoEnabled() && dto.getRedirectUris().stream().noneMatch(RedirectDTO::isAvailable)) throw new BizException(400,"启用SSO必须至少配置一个有效回调");
        long id;
        if (dto.getId()==null) {
            id=IdWorker.getId();
            store.jdbc().update("INSERT INTO sys_application (id,code,name,description,icon_url,entry_url,service_id,status,sso_enabled,sort,create_user,modify_user) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    id,dto.getCode(),dto.getName(),dto.getDescription(),dto.getIconUrl(),dto.getEntryUrl(),dto.getServiceId(),dto.getStatus(),dto.isSsoEnabled(),dto.getSort(),String.valueOf(boundary.userId()),String.valueOf(boundary.userId()));
        } else {
            id=dto.getId();
            var old=store.one("SELECT * FROM sys_application WHERE id=? AND is_delete=0 FOR UPDATE",id);
            if (!old.get("code").equals(dto.getCode()) || !old.get("service_id").equals(dto.getServiceId())) throw new BizException(400,"已发布的应用编码与服务标识不可修改");
            if (id==1 && (!"ENABLED".equals(dto.getStatus()) || dto.isSsoEnabled())) throw new BizException(400,"平台壳不能停用或改为外部SSO应用");
            changed(store.jdbc().update("UPDATE sys_application SET name=?,description=?,icon_url=?,entry_url=?,status=?,sso_enabled=?,sort=?,version=version+1,modify_user=? WHERE id=? AND version=? AND is_delete=0",
                    dto.getName(),dto.getDescription(),dto.getIconUrl(),dto.getEntryUrl(),dto.getStatus(),dto.isSsoEnabled(),dto.getSort(),String.valueOf(boundary.userId()),id,version(dto.getExpectedVersion())));
            // Configuration changes invalidate outstanding sessions; issued codes recheck the whitelist on exchange.
            if (id!=1) revocations.application(id,"APP_CONFIG_CHANGED");
        }
        store.jdbc().update("DELETE FROM sys_application_redirect_uri WHERE application_id=?",id);
        for (var redirect:dto.getRedirectUris()) store.jdbc().update("INSERT INTO sys_application_redirect_uri (id,application_id,environment,redirect_uri,logout_uri,available) VALUES (?,?,?,?,?,?)",
                IdWorker.getId(),id,redirect.getEnvironment(),redirect.getRedirectUri(),redirect.getLogoutUri(),redirect.isAvailable());
        return id;
    }

    @Transactional
    @OperateLog(module="应用管理",action="下架删除应用",type=OperateType.DELETE,maskParams=true)
    public void remove(long id,int version) {
        boundary.require("manage");
        if (id==1) throw new BizException(400,"不能删除平台壳");
        store.one("SELECT id FROM sys_application WHERE id=? AND is_delete=0 FOR UPDATE",id);
        Long active=store.jdbc().queryForObject("SELECT COUNT(*) FROM sys_tenant_application WHERE application_id=? AND status<>'CLOSED' AND is_delete=0",Long.class,id);
        if (active!=null && active>0) throw new BizException(409,"请先关闭所有租户开通实例");
        changed(store.jdbc().update("UPDATE sys_application SET is_delete=1,status='OFFLINE',version=version+1 WHERE id=? AND version=? AND is_delete=0",id,version));
        revocations.application(id,"APP_DELETED");
    }

    @Transactional
    @OperateLog(module="应用管理",action="轮换应用客户端凭证",type=OperateType.AUTH,maskParams=true)
    public ClientSecretVO rotate(long id,int version) {
        boundary.require("manage");
        if (id==1) throw new BizException(400,"平台壳不使用外部客户端凭证");
        var row=store.one("SELECT service_id FROM sys_application WHERE id=? AND is_delete=0 FOR UPDATE",id);
        String secret=SsoCrypto.random();
        changed(store.jdbc().update("UPDATE sys_application SET client_secret_hash=?,version=version+1 WHERE id=? AND version=? AND is_delete=0",SsoCrypto.hash(secret),id,version));
        revocations.application(id,"CLIENT_SECRET_ROTATED");
        var vo=new ClientSecretVO(); vo.setClientId((String)row.get("service_id")); vo.setClientSecret(secret); vo.setVersion(version+1); return vo;
    }
}
