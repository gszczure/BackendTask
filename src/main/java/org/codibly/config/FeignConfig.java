package org.codibly.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor myRequestInterceptor() {
        return template -> template.uri(template.path().replaceAll("%3A", ":"));
    }
}
