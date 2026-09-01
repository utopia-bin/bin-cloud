package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.application.SysApplication;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 应用域持久层 Mapper。
 *
 * <p>单表应用目录操作继承 MyBatis-Plus，跨表查询和原子状态更新统一定义在 XML 中。
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationPersistenceMapper extends BaseMapper<SysApplication> {}
