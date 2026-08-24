package cn.utopiabin.cloud.api.admin.controller.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.system.SysDictApi;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictOptionsCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictOptionsPageQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictOptionsUpdateDTO;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictPageQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsItemVO;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsMulTreeVO;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsTreeVO;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsVO;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 系统字典及字典项管理 REST 接口。 */
@Tag(name = "系统字典")
@Validated
@RestController
@RequestMapping("/dicts")
public class SysDictController {

    @DubboReference
    private SysDictApi dictApi;

    @Operation(summary = "新增字典")
    @PostMapping
    public RestResult<Void> createDict(@Valid @RequestBody SysDictCreateDTO dto) {
        dictApi.createDict(dto);
        return RestResult.ok();
    }

    @Operation(summary = "编辑字典")
    @PutMapping
    public RestResult<Void> updateDict(@Valid @RequestBody SysDictUpdateDTO dto) {
        dictApi.updateDict(dto);
        return RestResult.ok();
    }

    @Operation(summary = "删除字典")
    @DeleteMapping("/{id}")
    public RestResult<Void> removeDict(@PathVariable Long id) {
        dictApi.removeDict(id);
        return RestResult.ok();
    }

    @Operation(summary = "查询字典详情")
    @GetMapping("/{id}")
    public RestResult<SysDictVO> getDict(@PathVariable Long id) {
        return RestResult.ok(dictApi.getDict(id));
    }

    @Operation(summary = "分页查询字典")
    @GetMapping
    public RestResult<PageResult<SysDictVO>> pageDict(@Valid @ModelAttribute SysDictPageQuery query) {
        return RestResult.ok(dictApi.pageDict(query));
    }

    @Operation(summary = "列表查询字典")
    @GetMapping("/list")
    public RestResult<List<SysDictVO>> listDict(@Valid @ModelAttribute SysDictListQuery query) {
        return RestResult.ok(dictApi.listDict(query));
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/options")
    public RestResult<Void> createDictOption(@Valid @RequestBody SysDictOptionsCreateDTO dto) {
        dictApi.createDictOption(dto);
        return RestResult.ok();
    }

    @Operation(summary = "编辑字典项")
    @PutMapping("/options")
    public RestResult<Void> updateDictOption(@Valid @RequestBody SysDictOptionsUpdateDTO dto) {
        dictApi.updateDictOption(dto);
        return RestResult.ok();
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/options/{id}")
    public RestResult<Void> removeDictOption(@PathVariable Long id) {
        dictApi.removeDictOption(id);
        return RestResult.ok();
    }

    @Operation(summary = "查询字典项详情")
    @GetMapping("/options/{id}")
    public RestResult<SysDictOptionsVO> getDictOption(@PathVariable Long id) {
        return RestResult.ok(dictApi.getDictOption(id));
    }

    @Operation(summary = "分页查询字典项")
    @GetMapping("/options")
    public RestResult<PageResult<SysDictOptionsVO>> pageDictOption(
            @Valid @ModelAttribute SysDictOptionsPageQuery query) {
        return RestResult.ok(dictApi.pageDictOption(query));
    }

    @Operation(summary = "按编码获取字典项")
    @GetMapping("/codes/{dictCode}/items")
    public RestResult<List<SysDictOptionsItemVO>> getDictItems(@PathVariable String dictCode) {
        return RestResult.ok(dictApi.getDictItems(dictCode));
    }

    @Operation(summary = "刷新全部字典缓存")
    @PostMapping("/cache/refresh")
    public RestResult<Void> refreshDictCache() {
        dictApi.refreshDictCache();
        return RestResult.ok();
    }

    @Operation(summary = "按编码获取字典树")
    @GetMapping("/codes/{dictCode}/tree")
    public RestResult<List<SysDictOptionsTreeVO>> getDictTree(@PathVariable String dictCode) {
        return RestResult.ok(dictApi.getDictTree(dictCode));
    }

    @Operation(summary = "获取可选父级字典项")
    @GetMapping("/{dictId}/optional-parents")
    public RestResult<List<SysDictOptionsTreeVO>> getOptionalParents(
            @PathVariable Long dictId,
            @RequestParam(required = false) Long excludeId) {
        return RestResult.ok(dictApi.getOptionalParents(dictId, excludeId));
    }

    @Operation(summary = "获取多字典组合树")
    @GetMapping("/multi-tree")
    public RestResult<List<SysDictOptionsMulTreeVO>> getMulDictTree(@RequestParam String codes) {
        return RestResult.ok(dictApi.getMulDictTree(codes));
    }
}
