package cn.utopiabin.cloud.platform.api.impl.iam;

import cn.utopiabin.cloud.platform.api.iam.SysPermissionApi;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.model.dto.iam.SysPermissionCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysPermissionUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysPermissionVO;
import cn.utopiabin.cloud.platform.service.iam.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@DubboService
@RequiredArgsConstructor
public class SysPermissionApiImpl implements SysPermissionApi {
    private final SysPermissionService permissionService;

    @Override
    @RequirePermission("platform:permission:create")
    public Long create(SysPermissionCreateDTO dto) {
        return permissionService.create(dto);
    }

    @Override
    @RequirePermission("platform:permission:update")
    public void update(SysPermissionUpdateDTO dto) {
        permissionService.update(dto);
    }

    @Override
    @RequirePermission("platform:permission:delete")
    public void remove(Long id) {
        permissionService.remove(id);
    }

    @Override
    @RequirePermission("platform:permission:read")
    public SysPermissionVO get(Long id) {
        return permissionService.get(id);
    }

    @Override
    @RequirePermission("platform:permission:read")
    public List<SysPermissionVO> list() {
        return permissionService.list();
    }
}
