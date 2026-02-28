package io.walloop.engine.liquid.client;

import io.walloop.engine.liquid.config.LiquidRpcProperties;
import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquidRpcFeignConfig {

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor(LiquidRpcProperties properties) {
        return new BasicAuthRequestInterceptor(properties.getUsername(), properties.getPassword());
    }
}


