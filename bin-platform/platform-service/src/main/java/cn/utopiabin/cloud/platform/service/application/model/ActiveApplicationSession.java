package cn.utopiabin.cloud.platform.service.application.model;

import cn.utopiabin.cloud.platform.entity.application.SysSsoSession;
import cn.utopiabin.cloud.platform.repository.application.model.ApplicationAccessRecord;

/** 已完成令牌、会话和应用边界校验的身份上下文，仅用于服务内部。 */
public record ActiveApplicationSession(SysSsoSession session, ApplicationAccessRecord identity) {}
