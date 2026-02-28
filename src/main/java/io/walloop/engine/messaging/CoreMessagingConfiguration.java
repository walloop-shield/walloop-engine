package io.walloop.engine.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreMessagingConfiguration {

    public static final String CORE_EXCHANGE = "walloop.core.exchange";
    public static final String CORE_DEAD_LETTER_EXCHANGE = "walloop.core.dlx";

    @Bean
    public DirectExchange walloopCoreExchange() {
        return new DirectExchange(CORE_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange walloopCoreDeadLetterExchange() {
        return new DirectExchange(CORE_DEAD_LETTER_EXCHANGE, true, false);
    }
}

