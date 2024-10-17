package com.proxy.pureproxy.concreateproxy.code;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConCreateLogic {
    public String operation() {
        log.info("ConcreteLogic 실행");
        return "data";
    }
}
