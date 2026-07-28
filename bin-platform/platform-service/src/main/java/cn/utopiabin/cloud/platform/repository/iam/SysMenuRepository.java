package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.mapper.iam.SysMenuMapper;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuListQuery;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 系统菜单 Repository
 *
 * @since 1.0
 */
@Repository
public class SysMenuRepository extends BaseRepository<SysMenuMapper, SysMenu> {

    @Override
    protected String getNotFoundMessage() {
        return "菜单不存在";
    }

    /**
     * 是否有子菜单
     */
    public boolean hasChild(Long parentId) {
        return count(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId)) > 0;
    }

    /**
     * 列表查询
     */
    public List<SysMenu> list(SysMenuListQuery query) {
        var qw = new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort)
                .orderByDesc(SysMenu::getId);
        if (query != null) {
            qw.like(StrUtil.isNotBlank(query.getName()), SysMenu::getName, query.getName())
                    .eq(Objects.nonNull(query.getAvailable()), SysMenu::getAvailable, query.getAvailable());
        }
        return list(qw);
    }
}
