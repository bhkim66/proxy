package com.proxy.jdkdynamic;

import com.proxy.jdkdynamic.code.AImpl;
import com.proxy.jdkdynamic.code.AInterface;
import com.proxy.jdkdynamic.code.BInterface;
import com.proxy.jdkdynamic.code.TimeInvocationHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

@Slf4j
public class JdkDynamicProxyTest {

    @Test
    void dynamicA() {
        AInterface target = new AImpl();

        TimeInvocationHandler handler = new TimeInvocationHandler(target);

        AInterface proxy = (AInterface) Proxy.newProxyInstance(AInterface.class.getClassLoader(), new Class[]{AInterface.class}, handler);

        proxy.call();
        log.info("targetClass={} ", target.getClass());
        log.info("proxyClass={} ", proxy.getClass());
    }

    @Test
    void dynamicB() {
        AInterface target = new AImpl();

        TimeInvocationHandler handler = new TimeInvocationHandler(target);

        BInterface proxy = (BInterface) Proxy.newProxyInstance(BInterface.class.getClassLoader(), new Class[]{BInterface.class}, handler);

        proxy.call();
        log.info("targetClass={} ", target.getClass());
        log.info("proxyClass={} ", proxy.getClass());
    }
}
