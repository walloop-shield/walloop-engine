package com.walloop.engine.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreMessagingConfiguration {

    public static final String CORE_EXCHANGE = "walloop.core.exchange";

    @Bean
    public DirectExchange walloopCoreExchange() {
        return new DirectExchange(CORE_EXCHANGE, true, false);
    }
}
