package com.proxy.config.v1_proxy;

import com.proxy.app.v1.*;
import com.proxy.config.v1_proxy.interface_proxy.OrderControllerInterfaceProxy;
import com.proxy.config.v1_proxy.interface_proxy.OrderRepositoryInterfaceProxy;
import com.proxy.config.v1_proxy.interface_proxy.OrderServiceInterfaceProxy;
import com.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InterfaceProxyConfig {
    @Bean
    public OrderControllerV1 orderControllerV1(LogTrace logTrace) {
        OrderControllerV1Impl orderControllerV1 = new OrderControllerV1Impl(orderServiceV1(logTrace));
        return new OrderControllerInterfaceProxy(orderControllerV1, logTrace);
    }

    @Bean
    public OrderServiceV1 orderServiceV1(LogTrace logTrace) {
        OrderServiceV1Impl orderServiceV1 = new OrderServiceV1Impl(orderRepositoryV1(logTrace));
        return new OrderServiceInterfaceProxy(orderServiceV1, logTrace);
    }

    @Bean
    public OrderRepositoryV1 orderRepositoryV1(LogTrace logTrace) {
        OrderRepositoryV1Impl orderRepositoryInterfaceProxy = new OrderRepositoryV1Impl();
        return new OrderRepositoryInterfaceProxy(orderRepositoryInterfaceProxy, logTrace);
    }

}
