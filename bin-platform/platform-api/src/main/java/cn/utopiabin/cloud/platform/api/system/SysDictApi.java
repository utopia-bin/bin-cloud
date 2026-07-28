package cn.utopiabin.cloud.platform.api.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.system.*;
import cn.utopiabin.cloud.platform.model.vo.system.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 系统字典 Dubbo API
 * <p>
 * 提供字典及字典项的增删改查、缓存刷新、树形结构构建能力。
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "系统字典", description = "系统字典及字典项管理，支持树形结构与缓存刷新")
public interface SysDictApi {

    // ==================== 字典 CRUD ====================

    @Operation(summary = "新增字典", description = "字典名称与编码唯一性校验通过后新增")
    void createDict(@Parameter(description = "字典新增参数", required = true) SysDictCreateDTO dto);

    @Operation(summary = "编辑字典", description = "按ID编辑字典，编码变更时同步刷新缓存")
    void updateDict(@Parameter(description = "字典编辑参数", required = true) SysDictUpdateDTO dto);

    @Operation(summary = "删除字典", description = "删除字典及其下所有字典项，并清除缓存")
    void removeDict(@Parameter(description = "字典ID", required = true) Long id);

    @Operation(summary = "查询字典详情")
    SysDictVO getDict(@Parameter(description = "字典ID", required = true) Long id);

    @Operation(summary = "分页查询字典")
    PageResult<SysDictVO> pageDict(@Parameter(description = "分页查询条件", required = true) SysDictPageQuery query);

    @Operation(summary = "列表查询字典")
    List<SysDictVO> listDict(@Parameter(description = "列表查询条件") SysDictListQuery query);

    // ==================== 字典项 CRUD ====================

    @Operation(summary = "新增字典项", description = "同字典下名称与值唯一性校验通过后新增")
    void createDictOption(@Parameter(description = "字典项新增参数", required = true) SysDictOptionsCreateDTO dto);

    @Operation(summary = "编辑字典项", description = "按ID编辑字典项，并刷新所属字典缓存")
    void updateDictOption(@Parameter(description = "字典项编辑参数", required = true) SysDictOptionsUpdateDTO dto);

    @Operation(summary = "删除字典项", description = "存在子级字典项时不可删除")
    void removeDictOption(@Parameter(description = "字典项ID", required = true) Long id);

    @Operation(summary = "查询字典项详情")
    SysDictOptionsVO getDictOption(@Parameter(description = "字典项ID", required = true) Long id);

    @Operation(summary = "分页查询字典项")
    PageResult<SysDictOptionsVO> pageDictOption(@Parameter(description = "分页查询条件", required = true) SysDictOptionsPageQuery query);

    // ==================== 缓存 ====================

    @Operation(summary = "获取字典缓存项列表", description = "按字典编码从缓存获取，缓存不存在则查库并回填")
    List<SysDictOptionsItemVO> getDictItems(@Parameter(description = "字典编码", required = true) String dictCode);

    @Operation(summary = "刷新全部字典缓存", description = "清除所有字典相关缓存并重建")
    void refreshDictCache();

    // ==================== 树形 ====================

    @Operation(summary = "获取字典树", description = "将指定字典的字典项按父子关系构建为树形结构")
    List<SysDictOptionsTreeVO> getDictTree(@Parameter(description = "字典编码", required = true) String dictCode);

    @Operation(summary = "获取可选父级字典项", description = "获取某字典下可作为父级的字典项列表，排除自身及其子孙")
    List<SysDictOptionsTreeVO> getOptionalParents(
            @Parameter(description = "字典ID", required = true) Long dictId,
            @Parameter(description = "排除项ID（自身ID）") Long excludeId);

    @Operation(summary = "获取多字典组合树", description = "以逗号分隔多个字典编码，返回各字典对应的树形数据")
    List<SysDictOptionsMulTreeVO> getMulDictTree(@Parameter(description = "字典编码，逗号分隔", required = true) String codes);
}
