package com.walloop.engine.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineMessagingConfiguration {

    public static final String ENGINE_EXCHANGE = "walloop.engine.exchange";

    @Bean
    public DirectExchange walloopEngineExchange() {
        return new DirectExchange(ENGINE_EXCHANGE, true, false);
    }
}
