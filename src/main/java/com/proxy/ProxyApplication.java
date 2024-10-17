package com.proxy;

import com.proxy.config.AppV1Config;
import com.proxy.config.v1_proxy.InterfaceProxyConfig;
import com.trace.logtrace.LogTrace;
import com.trace.logtrace.ThreadLocalLogTrace;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(InterfaceProxyConfig.class)
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
