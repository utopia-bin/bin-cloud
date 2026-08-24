package cn.utopiabin.cloud.api.admin.controller.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.system.SysParameterApi;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterPageQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.system.SysParameterVO;
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

/** 系统参数管理 REST 接口。 */
@Tag(name = "系统参数")
@Validated
@RestController
@RequestMapping("/parameters")
public class SysParameterController {

    @DubboReference
    private SysParameterApi parameterApi;

    @Operation(summary = "新增系统参数")
    @PostMapping
    public RestResult<Void> create(@Valid @RequestBody SysParameterCreateDTO dto) {
        parameterApi.create(dto);
        return RestResult.ok();
    }

    @Operation(summary = "编辑系统参数")
    @PutMapping
    public RestResult<Void> update(@Valid @RequestBody SysParameterUpdateDTO dto) {
        parameterApi.update(dto);
        return RestResult.ok();
    }

    @Operation(summary = "删除系统参数")
    @DeleteMapping("/{id}")
    public RestResult<Void> remove(@PathVariable Long id) {
        parameterApi.remove(id);
        return RestResult.ok();
    }

    @Operation(summary = "分页查询系统参数")
    @GetMapping
    public RestResult<PageResult<SysParameterVO>> page(
            @Valid @ModelAttribute SysParameterPageQuery query) {
        return RestResult.ok(parameterApi.page(query));
    }

    @Operation(summary = "列表查询系统参数")
    @GetMapping("/list")
    public RestResult<List<SysParameterVO>> list(@Valid @ModelAttribute SysParameterListQuery query) {
        return RestResult.ok(parameterApi.list(query));
    }

    @Operation(summary = "按键获取参数值")
    @GetMapping("/value")
    public RestResult<String> getValue(@RequestParam String key,
                                       @RequestParam(required = false) String defaultValue) {
        return RestResult.ok(parameterApi.getValue(key, defaultValue));
    }

    @Operation(summary = "刷新全部参数缓存")
    @PostMapping("/cache/refresh")
    public RestResult<Void> refreshCache() {
        parameterApi.refreshCache();
        return RestResult.ok();
    }
}
