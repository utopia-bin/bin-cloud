package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.requireSingleChange;
import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.requireVersion;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.RedirectDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationVO;
import cn.utopiabin.cloud.platform.model.vo.application.ClientSecretVO;
import cn.utopiabin.cloud.platform.repository.application.ApplicationCatalogRepository;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class ApplicationCatalogService {
    private final ApplicationCatalogRepository repository;
    private final ApplicationBoundary boundary;
    private final ApplicationRevocationService revocations;

    @RequirePermission("platform:application:read")
    public PageResult<ApplicationVO> page(ApplicationQuery q) {
        return repository.page(q);
    }

    @RequirePermission("platform:application:read")
    public ApplicationVO get(long id) {
        var result = repository.find(id);
        if (result.isEmpty()) throw new BizException(404, "应用不存在");
        var vo = result.getFirst();
        vo.setRedirectUris(repository.listRedirects(id));
        return vo;
    }

    @Transactional
    @RequirePermission("platform:application:manage")
    @OperateLog(module = "应用管理", action = "保存应用产品", type = OperateType.UPDATE, maskParams = true)
    public long save(ApplicationDTO dto) {
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
            repository.insert(id, dto, boundary.userId());
        } else {
            id = dto.getId();
            var old = repository.lock(id);
            if (!old.get("code").equals(dto.getCode())
                    || !old.get("service_id").equals(dto.getServiceId()))
                throw new BizException(400, "已发布的应用编码与服务标识不可修改");
            if (id == 1 && (!"ENABLED".equals(dto.getStatus()) || dto.isSsoEnabled()))
                throw new BizException(400, "平台壳不能停用或改为外部SSO应用");
            requireSingleChange(
                    repository.update(
                            id, dto, boundary.userId(), requireVersion(dto.getExpectedVersion())));
            // 应用配置变化后立即撤销现有会话；已签发的授权码在兑换时还会再次校验回调白名单。
            if (id != 1) revocations.application(id, "APP_CONFIG_CHANGED");
        }
        repository.replaceRedirects(id, dto.getRedirectUris());
        return id;
    }

    @Transactional
    @RequirePermission("platform:application:manage")
    @OperateLog(module = "应用管理", action = "下架删除应用", type = OperateType.DELETE, maskParams = true)
    public void remove(long id, int version) {
        if (id == 1) throw new BizException(400, "不能删除平台壳");
        repository.requireExisting(id);
        if (repository.countActiveInstances(id) > 0) {
            throw new BizException(409, "请先关闭所有租户开通实例");
        }
        requireSingleChange(repository.remove(id, version));
        revocations.application(id, "APP_DELETED");
    }

    @Transactional
    @RequirePermission("platform:application:manage")
    @OperateLog(module = "应用管理", action = "轮换应用客户端凭证", type = OperateType.AUTH, maskParams = true)
    public ClientSecretVO rotate(long id, int version) {
        if (id == 1) throw new BizException(400, "平台壳不使用外部客户端凭证");
        var row = repository.lockService(id);
        String secret = SsoCrypto.random();
        requireSingleChange(repository.updateClientSecret(id, version, SsoCrypto.hash(secret)));
        revocations.application(id, "CLIENT_SECRET_ROTATED");
        var vo = new ClientSecretVO();
        vo.setClientId((String) row.get("service_id"));
        vo.setClientSecret(secret);
        vo.setVersion(version + 1);
        return vo;
    }
}
