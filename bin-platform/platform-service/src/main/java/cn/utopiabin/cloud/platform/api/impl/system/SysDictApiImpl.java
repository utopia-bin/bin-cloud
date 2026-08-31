package cn.utopiabin.cloud.platform.api.impl.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.api.system.SysDictApi;
import cn.utopiabin.cloud.platform.model.dto.system.*;
import cn.utopiabin.cloud.platform.model.vo.system.*;
import cn.utopiabin.cloud.platform.service.system.SysDictService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 系统字典 API 实现
 * <p>
 * 委托 {@link SysDictService} 处理业务逻辑。
 *
 * @since 1.0
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
@Tag(name = "系统字典", description = "系统字典及字典项 Dubbo 服务实现")
public class SysDictApiImpl implements SysDictApi {

    private final SysDictService dictService;

    // ==================== 字典 CRUD ====================

    @Override
    @RequirePermission("platform:dict:create")
    public void createDict(SysDictCreateDTO dto) {
        dictService.createDict(dto);
    }

    @Override
    @RequirePermission("platform:dict:update")
    public void updateDict(SysDictUpdateDTO dto) {
        dictService.updateDict(dto);
    }

    @Override
    @RequirePermission("platform:dict:delete")
    public void removeDict(Long id) {
        dictService.removeDict(id);
    }

    @Override
    @RequirePermission("platform:dict:read")
    public SysDictVO getDict(Long id) {
        return dictService.getDict(id);
    }

    @Override
    @RequirePermission("platform:dict:read")
    public PageResult<SysDictVO> pageDict(SysDictPageQuery query) {
        return dictService.pageDict(query);
    }

    @Override
    @RequirePermission("platform:dict:read")
    public List<SysDictVO> listDict(SysDictListQuery query) {
        return dictService.listDict(query);
    }

    // ==================== 字典项 CRUD ====================

    @Override
    @RequirePermission("platform:dict:create")
    public void createDictOption(SysDictOptionsCreateDTO dto) {
        dictService.createDictOption(dto);
    }

    @Override
    @RequirePermission("platform:dict:update")
    public void updateDictOption(SysDictOptionsUpdateDTO dto) {
        dictService.updateDictOption(dto);
    }

    @Override
    @RequirePermission("platform:dict:delete")
    public void removeDictOption(Long id) {
        dictService.removeDictOption(id);
    }

    @Override
    @RequirePermission("platform:dict:read")
    public SysDictOptionsVO getDictOption(Long id) {
        return dictService.getDictOption(id);
    }

    @Override
    @RequirePermission("platform:dict:read")
    public PageResult<SysDictOptionsVO> pageDictOption(SysDictOptionsPageQuery query) {
        return dictService.pageDictOption(query);
    }

    // ==================== 缓存 ====================

    @Override
    @RequirePermission("platform:dict:read")
    public List<SysDictOptionsItemVO> getDictItems(String dictCode) {
        return dictService.getDictItems(dictCode);
    }

    @Override
    @RequirePermission("platform:dict:update")
    public void refreshDictCache() {
        dictService.refreshDictCache();
    }

    // ==================== 树形 ====================

    @Override
    @RequirePermission("platform:dict:read")
    public List<SysDictOptionsTreeVO> getDictTree(String dictCode) {
        return dictService.getDictTree(dictCode);
    }

    @Override
    @RequirePermission("platform:dict:read")
    public List<SysDictOptionsTreeVO> getOptionalParents(Long dictId, Long excludeId) {
        return dictService.getOptionalParents(dictId, excludeId);
    }

    @Override
    @RequirePermission("platform:dict:read")
    public List<SysDictOptionsMulTreeVO> getMulDictTree(String codes) {
        return dictService.getMulDictTree(codes);
    }
}
