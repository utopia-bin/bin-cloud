package cn.utopiabin.cloud.api.admin.controller.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.system.SysOperateLogApi;
import cn.utopiabin.cloud.platform.model.dto.system.SysOperateLogPageQuery;
import cn.utopiabin.cloud.platform.model.vo.system.SysOperateLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志审计 REST 接口。 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/operate-logs")
public class SysOperateLogController {

    @DubboReference
    private SysOperateLogApi operateLogApi;

    @Operation(summary = "分页查询操作日志")
    @GetMapping
    public RestResult<PageResult<SysOperateLogVO>> page(
            @Valid @ModelAttribute SysOperateLogPageQuery query) {
        return RestResult.ok(operateLogApi.page(query));
    }
}
