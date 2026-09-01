package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationPersistenceMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.RedirectDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationVO;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 应用产品目录数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationCatalogRepository extends ApplicationRepositorySupport {
    private final ApplicationPersistenceMapper mapper;

    public PageResult<ApplicationVO> page(ApplicationQuery query) {
        Map<String, Object> parameters = pageParameters(query);
        Long total = scalarLong(mapper.catalogCount(parameters));
        return page(query, total, mapper.catalogPage(parameters), ApplicationVO.class);
    }

    public List<ApplicationVO> find(long applicationId) {
        return convert(
                mapper.selectApplicationDetail(parameters(applicationId)), ApplicationVO.class);
    }

    public List<RedirectDTO> listRedirects(long applicationId) {
        return convert(
                mapper.selectApplicationRedirects(parameters(applicationId)), RedirectDTO.class);
    }

    public Map<String, Object> lock(long applicationId) {
        return one(mapper.selectApplicationForUpdate(parameters(applicationId)));
    }

    public void requireExisting(long applicationId) {
        one(mapper.selectApplicationIdForUpdate(parameters(applicationId)));
    }

    public Map<String, Object> lockService(long applicationId) {
        return one(mapper.selectApplicationServiceForUpdate(parameters(applicationId)));
    }

    public long countActiveInstances(long applicationId) {
        Long count = scalarLong(mapper.countActiveTenantApplications(parameters(applicationId)));
        return count == null ? 0 : count;
    }

    public int insert(long applicationId, ApplicationDTO dto, long operatorId) {
        return mapper.insertApplication(
                parameters(
                        applicationId,
                        dto.getCode(),
                        dto.getName(),
                        dto.getDescription(),
                        dto.getIconUrl(),
                        dto.getEntryUrl(),
                        dto.getServiceId(),
                        dto.getStatus(),
                        dto.isSsoEnabled(),
                        dto.getSort(),
                        String.valueOf(operatorId),
                        String.valueOf(operatorId)));
    }

    public int update(long applicationId, ApplicationDTO dto, long operatorId, int version) {
        return mapper.updateApplication(
                parameters(
                        dto.getName(),
                        dto.getDescription(),
                        dto.getIconUrl(),
                        dto.getEntryUrl(),
                        dto.getStatus(),
                        dto.isSsoEnabled(),
                        dto.getSort(),
                        String.valueOf(operatorId),
                        applicationId,
                        version));
    }

    public void replaceRedirects(long applicationId, List<RedirectDTO> redirects) {
        mapper.deleteApplicationRedirects(parameters(applicationId));
        for (RedirectDTO redirect : redirects) {
            mapper.insertApplicationRedirect(
                    parameters(
                            IdWorker.getId(),
                            applicationId,
                            redirect.getEnvironment(),
                            redirect.getRedirectUri(),
                            redirect.getLogoutUri(),
                            redirect.isAvailable()));
        }
    }

    public int remove(long applicationId, int version) {
        return mapper.softDeleteApplication(parameters(applicationId, version));
    }

    public int updateClientSecret(long applicationId, int version, String secretHash) {
        return mapper.updateApplicationClientSecret(parameters(secretHash, applicationId, version));
    }
}
