package com.proxy.common.advice;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@Slf4j
public class TimeAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        log.info("TimeDecorator 실행");

        long startTime = System.currentTimeMillis();

        //타켓 클래스를 호출하고 그결과를 받는다 / 타켓 클래스 정보가 MethodInvocation invocation 들어있다
        Object result = invocation.proceed();

        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("TimeDecorator 종료 resultTime={}", resultTime);

        return result;
    }
}
