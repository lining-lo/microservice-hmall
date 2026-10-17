package com.heima.api.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.heima.api.client", defaultConfiguration = DefaultFeignConfig.class)
public class ApiAutoConfiguration {
}
