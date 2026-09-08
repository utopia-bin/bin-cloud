package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.exception.BizException;

/** 应用领域查询的缺失记录处理。 */
abstract class ApplicationRepositorySupport {
  protected <T> T require(T value) {
    if (value == null) {
      // 同时隐藏记录不存在和越界两种情况，避免暴露其他租户的数据。
      throw new BizException(404, "记录不存在或不属于当前授权范围");
    }
    return value;
  }
}
