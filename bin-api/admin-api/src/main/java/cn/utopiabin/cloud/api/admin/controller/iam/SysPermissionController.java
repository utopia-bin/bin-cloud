package cn.utopiabin.cloud.api.admin.controller.iam;

import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.iam.SysPermissionApi;
import cn.utopiabin.cloud.platform.model.dto.iam.SysPermissionCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysPermissionUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysPermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 系统权限资源管理 REST 接口。 */
@Tag(name = "系统权限资源")
@RestController
@RequestMapping("/permissions")
public class SysPermissionController {

    @DubboReference
    private SysPermissionApi permissionApi;

    @Operation(summary = "新增权限资源")
    @PostMapping
    public RestResult<Long> create(@Valid @RequestBody SysPermissionCreateDTO dto) {
        return RestResult.ok(permissionApi.create(dto));
    }

    @Operation(summary = "编辑权限资源")
    @PutMapping
    public RestResult<Void> update(@Valid @RequestBody SysPermissionUpdateDTO dto) {
        permissionApi.update(dto);
        return RestResult.ok();
    }

    @Operation(summary = "删除权限资源")
    @DeleteMapping("/{id}")
    public RestResult<Void> remove(@PathVariable Long id) {
        permissionApi.remove(id);
        return RestResult.ok();
    }

    @Operation(summary = "查询权限资源详情")
    @GetMapping("/{id}")
    public RestResult<SysPermissionVO> get(@PathVariable Long id) {
        return RestResult.ok(permissionApi.get(id));
    }

    @Operation(summary = "查询全部权限资源")
    @GetMapping
    public RestResult<List<SysPermissionVO>> list() {
        return RestResult.ok(permissionApi.list());
    }
}
