package cn.utopiabin.cloud.platform.api.iam;

import cn.utopiabin.cloud.platform.model.dto.iam.SysPermissionCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysPermissionUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysPermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 系统权限资源 Dubbo API
 * <p>
 * 维护系统细粒度权限资源目录。权限资源通过权限编码与角色、菜单建立关联，
 * 是服务端鉴权和菜单导航投影的统一依据。
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "系统权限资源", description = "权限资源目录管理，含CRUD和列表查询")
public interface SysPermissionApi {

    @Operation(summary = "新增权限资源", description = "权限编码全局唯一，格式为 domain:resource:action")
    Long create(@Parameter(description = "权限资源新增参数", required = true) SysPermissionCreateDTO dto);

    @Operation(summary = "编辑权限资源", description = "使用乐观锁版本号更新；被菜单引用时不可修改权限编码")
    void update(@Parameter(description = "权限资源编辑参数", required = true) SysPermissionUpdateDTO dto);

    @Operation(summary = "删除权限资源", description = "已分配给角色或被菜单引用的权限资源不可删除")
    void remove(@Parameter(description = "权限资源ID", required = true) Long id);

    @Operation(summary = "查询权限资源详情")
    SysPermissionVO get(@Parameter(description = "权限资源ID", required = true) Long id);

    @Operation(summary = "查询全部权限资源", description = "按排序值和权限编码返回权限资源目录")
    List<SysPermissionVO> list();
}
