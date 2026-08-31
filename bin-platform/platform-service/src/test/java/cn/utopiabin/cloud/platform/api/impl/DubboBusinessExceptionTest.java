package cn.utopiabin.cloud.platform.api.impl;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.api.system.SysDictApi;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.filter.ExceptionFilter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DubboBusinessExceptionTest {
    @Test
    @SuppressWarnings("unchecked")
    void declaredBusinessExceptionSurvivesProviderExceptionFilter() {
        Invoker<SysDictApi> invoker = mock(Invoker.class);
        when(invoker.getInterface()).thenReturn(SysDictApi.class);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("getDict");
        when(invocation.getParameterTypes()).thenReturn(new Class<?>[]{Long.class});
        var exception = new BizException(404, "字典不存在");
        var response = new AppResponse(exception);
        new ExceptionFilter().onResponse(response, invoker, invocation);
        assertThat(response.getException()).isSameAs(exception);
    }
}
