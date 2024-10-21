package com.proxy;

import com.proxy.config.AppV1Config;
import com.proxy.config.BeanPostProcessorConfig;
import com.proxy.config.v1_proxy.InterfaceProxyConfig;
import com.proxy.config.v3_proxyfactory.ProxyFactoryConfigV1;
import com.proxy.config.v3_proxyfactory.ProxyFactoryConfigV2;
import com.proxy.config.v5_autoproxy.AutoProxyConfig;
import com.trace.logtrace.LogTrace;
import com.trace.logtrace.ThreadLocalLogTrace;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.swing.*;

//@Import(InterfaceProxyConfig.class)
//@Import(ProxyFactoryConfigV2.class)
//@Import(BeanPostProcessorConfig.class)
@Import(AutoProxyConfig.class)
@SpringBootApplication(scanBasePackages = "com.proxy.app.v3")
public class ProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProxyApplication.class, args);
	}

	@Bean
	public LogTrace logTrace() {
		return new ThreadLocalLogTrace();
	}
}
