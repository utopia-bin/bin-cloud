package cn.utopiabin.cloud.platform.api.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.system.*;
import cn.utopiabin.cloud.platform.model.vo.system.*;

import java.util.List;

/**
 * 系统字典 Dubbo API
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
public interface SysDictApi {

    // ==================== 字典 CRUD ====================

    /**
     * 新增/编辑字典
     */
    void saveDict(SysDictSaveDTO dto);

    /**
     * 删除字典（含字典项）
     */
    void removeDict(Long id);

    /**
     * 查询字典详情
     */
    SysDictVO getDict(Long id);

    /**
     * 分页查询字典
     */
    PageResult<SysDictVO> pageDict(SysDictPageQuery query);

    /**
     * 列表查询字典
     */
    List<SysDictVO> listDict(SysDictListQuery query);

    // ==================== 字典项 CRUD ====================

    /**
     * 新增/编辑字典项
     */
    void saveDictOption(SysDictOptionsSaveDTO dto);

    /**
     * 删除字典项
     */
    void removeDictOption(Long id);

    /**
     * 查询字典项详情
     */
    SysDictOptionsVO getDictOption(Long id);

    /**
     * 分页查询字典项
     */
    PageResult<SysDictOptionsVO> pageDictOption(SysDictOptionsPageQuery query);

    // ==================== 缓存 ====================

    /**
     * 获取字典缓存项列表
     */
    List<SysDictOptionsItemVO> getDictItems(String dictCode);

    /**
     * 刷新全部字典缓存
     */
    void refreshDictCache();

    // ==================== 树形 ====================

    /**
     * 获取字典树
     */
    List<SysDictOptionsTreeVO> getDictTree(String dictCode);

    /**
     * 获取可选父级字典项
     */
    List<SysDictOptionsTreeVO> getOptionalParents(Long dictId, Long excludeId);

    /**
     * 获取多字典组合树
     */
    List<SysDictOptionsMulTreeVO> getMulDictTree(String codes);
}
