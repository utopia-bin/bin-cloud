package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import cn.utopiabin.cloud.platform.mapper.iam.SysPermissionMapper;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SysPermissionRepository extends BaseRepository<SysPermissionMapper, SysPermission> {
    @Override
    protected String getNotFoundMessage() {
        return "权限资源不存在";
    }

    public List<SysPermission> listAll() {
        return list(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getCode));
    }
}
