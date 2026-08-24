package cn.utopiabin.cloud.api.admin.controller.iam;

import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.iam.SysMenuApi;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuListQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuVO;
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

/** 系统菜单管理 REST 接口。 */
@Tag(name = "系统菜单")
@Validated
@RestController
@RequestMapping("/menus")
public class SysMenuController {

    @DubboReference
    private SysMenuApi menuApi;

    @Operation(summary = "新增菜单")
    @PostMapping
    public RestResult<Long> create(@Valid @RequestBody SysMenuCreateDTO dto) {
        return RestResult.ok(menuApi.create(dto));
    }

    @Operation(summary = "编辑菜单")
    @PutMapping
    public RestResult<Void> update(@Valid @RequestBody SysMenuUpdateDTO dto) {
        menuApi.update(dto);
        return RestResult.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public RestResult<Void> remove(@PathVariable Long id) {
        menuApi.remove(id);
        return RestResult.ok();
    }

    @Operation(summary = "批量删除菜单")
    @DeleteMapping
    public RestResult<Void> batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        menuApi.batchDelete(dto);
        return RestResult.ok();
    }

    @Operation(summary = "查询菜单详情")
    @GetMapping("/{id}")
    public RestResult<SysMenuVO> get(@PathVariable Long id) {
        return RestResult.ok(menuApi.get(id));
    }

    @Operation(summary = "列表查询菜单")
    @GetMapping
    public RestResult<List<SysMenuVO>> list(@Valid @ModelAttribute SysMenuListQuery query) {
        return RestResult.ok(menuApi.list(query));
    }

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    public RestResult<List<SysMenuTreeVO>> tree(@Valid @ModelAttribute SysMenuListQuery query) {
        return RestResult.ok(menuApi.tree(query));
    }

    @Operation(summary = "根据权限码获取菜单")
    @GetMapping("/by-permissions")
    public RestResult<List<SysMenuVO>> listByPermissionCodes(
            @RequestParam List<String> permissionCodes) {
        return RestResult.ok(menuApi.listByPermissionCodes(permissionCodes));
    }
}
