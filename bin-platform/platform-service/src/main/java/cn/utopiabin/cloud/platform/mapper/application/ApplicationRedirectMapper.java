package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.application.SysApplicationRedirect;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 应用回调地址 Mapper。 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationRedirectMapper extends BaseMapper<SysApplicationRedirect> {

  /** 替换回调集合前物理删除旧记录，避免逻辑删除记录持续占用唯一键。 */
  int physicallyDeleteByApplication(@Param("applicationId") long applicationId);
}
