package com.walloop.engine.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineMessagingConfiguration {

    public static final String ENGINE_EXCHANGE = "walloop.engine.exchange";
    public static final String ENGINE_DEAD_LETTER_EXCHANGE = "walloop.engine.dlx";

    @Bean
    public DirectExchange walloopEngineExchange() {
        return new DirectExchange(ENGINE_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange walloopEngineDeadLetterExchange() {
        return new DirectExchange(ENGINE_DEAD_LETTER_EXCHANGE, true, false);
    }
}
