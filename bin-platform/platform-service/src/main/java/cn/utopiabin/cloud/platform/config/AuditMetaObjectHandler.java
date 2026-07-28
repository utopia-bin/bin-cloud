package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis-Plus 审计字段自动填充处理器
 * <p>
 * 配合 {@link cn.utopiabin.cloud.common.model.entity.BaseEntity} 中的
 * {@code @TableField(fill = FieldFill.INSERT/INSERT_UPDATE)} 注解使用，
 * 自动填充以下字段:
 * <ul>
 *   <li>插入时: gmtCreate, gmtModify, createUser, modifyUser, tenantId</li>
 *   <li>更新时: gmtModify, modifyUser</li>
 * </ul>
 * <p>
 * 用户信息从 {@link UserContextHolder} 获取 (经由网关 JwtAuthFilter → 请求头 →
 * UserContextFilter/DubboFilter → ThreadLocal 透传)。无用户上下文时填充 "system"。
 *
 * @since 1.0
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /** 无用户上下文时的默认操作人 */
    private static final String DEFAULT_USER = "system";

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        String currentUser = currentUser();

        // 审计字段 (BaseEntity 标注了 @TableField(fill = FieldFill.INSERT))
        this.strictInsertFill(metaObject, "gmtCreate", Date.class, now);
        this.strictInsertFill(metaObject, "gmtModify", Date.class, now);
        this.strictInsertFill(metaObject, "createUser", String.class, currentUser);
        this.strictInsertFill(metaObject, "modifyUser", String.class, currentUser);

        // tenantId (BaseEntity 未标注 fill 注解, 手动填充, 已有值不覆盖)
        fillTenantIdIfAbsent(metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 审计字段 (BaseEntity 标注了 @TableField(fill = FieldFill.INSERT_UPDATE))
        this.strictUpdateFill(metaObject, "gmtModify", Date.class, new Date());
        this.strictUpdateFill(metaObject, "modifyUser", String.class, currentUser());
    }

    /**
     * 自动填充 tenantId (仅插入时, 已有值不覆盖)
     * <p>
     * BaseEntity 中 tenantId 未标注 {@code @TableField(fill = ...)},
     * 因此 strictInsertFill 不会处理, 需手动检查并填充。
     */
    private void fillTenantIdIfAbsent(MetaObject metaObject) {
        // 检查实体是否包含 tenantId 属性 (LinkEntity 等不含)
        if (!metaObject.hasSetter("tenantId")) {
            return;
        }
        Object existing = getFieldValByName("tenantId", metaObject);
        if (existing != null) {
            return;
        }
        if (!UserContextHolder.isPresent()) {
            return;
        }
        String tenantIdStr = UserContextHolder.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            return;
        }
        try {
            setFieldValByName("tenantId", Long.valueOf(tenantIdStr), metaObject);
        } catch (NumberFormatException e) {
            log.warn("tenantId 自动填充失败, 值非数字: {}", tenantIdStr);
        }
    }

    /**
     * 获取当前操作人, 优先用户名, 其次用户ID, 最后默认值
     */
    private String currentUser() {
        if (!UserContextHolder.isPresent()) {
            return DEFAULT_USER;
        }
        String username = UserContextHolder.getUsername();
        if (username != null && !username.isBlank()) {
            return username;
        }
        String userId = UserContextHolder.getUserId();
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        return DEFAULT_USER;
    }
}
