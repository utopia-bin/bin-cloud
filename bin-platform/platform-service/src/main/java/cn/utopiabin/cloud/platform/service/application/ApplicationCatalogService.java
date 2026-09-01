package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.changed;
import static cn.utopiabin.cloud.platform.service.application.ApplicationStore.version;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.RedirectDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationVO;
import cn.utopiabin.cloud.platform.model.vo.application.ClientSecretVO;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class ApplicationCatalogService {
    private final ApplicationStore store;
    private final ApplicationBoundary boundary;
    private final ApplicationRevocationService revocations;

    public PageResult<ApplicationVO> page(ApplicationQuery q) {
        boundary.require("read");
        return store.page(ApplicationVO.class, q, "catalogCount", "catalogPage");
    }

    public ApplicationVO get(long id) {
        boundary.require("read");
        var result = store.list(ApplicationVO.class, "applicationCatalogServiceSelect04", id);
        if (result.isEmpty()) throw new BizException(404, "应用不存在");
        var vo = result.getFirst();
        vo.setRedirectUris(store.list(RedirectDTO.class, "applicationCatalogServiceSelect05", id));
        return vo;
    }

    @Transactional
    @OperateLog(module = "应用管理", action = "保存应用产品", type = OperateType.UPDATE, maskParams = true)
    public long save(ApplicationDTO dto) {
        boundary.require("manage");
        SsoCrypto.navigation(dto.getEntryUrl(), false);
        SsoCrypto.navigation(dto.getIconUrl(), true);
        var seen = new HashSet<String>();
        for (var redirect : dto.getRedirectUris()) {
            SsoCrypto.redirect(redirect.getRedirectUri(), redirect.getEnvironment());
            if (!redirect.getLogoutUri().isBlank())
                SsoCrypto.redirect(redirect.getLogoutUri(), redirect.getEnvironment());
            if (!seen.add(redirect.getRedirectUri())) throw new BizException(400, "回调地址不能重复");
        }
        if (dto.isSsoEnabled()
                && dto.getRedirectUris().stream().noneMatch(RedirectDTO::isAvailable))
            throw new BizException(400, "启用SSO必须至少配置一个有效回调");
        long id;
        if (dto.getId() == null) {
            id = IdWorker.getId();
            store.update(
                    "applicationCatalogServiceUpdate07",
                    id,
                    dto.getCode(),
                    dto.getName(),
                    dto.getDescription(),
                    dto.getIconUrl(),
                    dto.getEntryUrl(),
                    dto.getServiceId(),
                    dto.getStatus(),
                    dto.isSsoEnabled(),
                    dto.getSort(),
                    String.valueOf(boundary.userId()),
                    String.valueOf(boundary.userId()));
        } else {
            id = dto.getId();
            var old = store.one("applicationCatalogServiceSelect01", id);
            if (!old.get("code").equals(dto.getCode())
                    || !old.get("service_id").equals(dto.getServiceId()))
                throw new BizException(400, "已发布的应用编码与服务标识不可修改");
            if (id == 1 && (!"ENABLED".equals(dto.getStatus()) || dto.isSsoEnabled()))
                throw new BizException(400, "平台壳不能停用或改为外部SSO应用");
            changed(
                    store.update(
                            "applicationCatalogServiceUpdate08",
                            dto.getName(),
                            dto.getDescription(),
                            dto.getIconUrl(),
                            dto.getEntryUrl(),
                            dto.getStatus(),
                            dto.isSsoEnabled(),
                            dto.getSort(),
                            String.valueOf(boundary.userId()),
                            id,
                            version(dto.getExpectedVersion())));
            // 应用配置变化后立即撤销现有会话；已签发的授权码在兑换时还会再次校验回调白名单。
            if (id != 1) revocations.application(id, "APP_CONFIG_CHANGED");
        }
        store.update("applicationCatalogServiceUpdate09", id);
        for (var redirect : dto.getRedirectUris())
            store.update(
                    "applicationCatalogServiceUpdate10",
                    IdWorker.getId(),
                    id,
                    redirect.getEnvironment(),
                    redirect.getRedirectUri(),
                    redirect.getLogoutUri(),
                    redirect.isAvailable());
        return id;
    }

    @Transactional
    @OperateLog(module = "应用管理", action = "下架删除应用", type = OperateType.DELETE, maskParams = true)
    public void remove(long id, int version) {
        boundary.require("manage");
        if (id == 1) throw new BizException(400, "不能删除平台壳");
        store.one("applicationCatalogServiceSelect02", id);
        Long active = store.queryForObject("applicationCatalogServiceSelect06", Long.class, id);
        if (active != null && active > 0) throw new BizException(409, "请先关闭所有租户开通实例");
        changed(store.update("applicationCatalogServiceUpdate11", id, version));
        revocations.application(id, "APP_DELETED");
    }

    @Transactional
    @OperateLog(module = "应用管理", action = "轮换应用客户端凭证", type = OperateType.AUTH, maskParams = true)
    public ClientSecretVO rotate(long id, int version) {
        boundary.require("manage");
        if (id == 1) throw new BizException(400, "平台壳不使用外部客户端凭证");
        var row = store.one("applicationCatalogServiceSelect03", id);
        String secret = SsoCrypto.random();
        changed(
                store.update(
                        "applicationCatalogServiceUpdate12", SsoCrypto.hash(secret), id, version));
        revocations.application(id, "CLIENT_SECRET_ROTATED");
        var vo = new ClientSecretVO();
        vo.setClientId((String) row.get("service_id"));
        vo.setClientSecret(secret);
        vo.setVersion(version + 1);
        return vo;
    }
}
