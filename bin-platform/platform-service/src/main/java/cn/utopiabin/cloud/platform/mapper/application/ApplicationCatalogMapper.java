package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.application.SysApplication;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 全局应用产品目录 Mapper，不追加租户或平台壳过滤。 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationCatalogMapper extends BaseMapper<SysApplication> {}
