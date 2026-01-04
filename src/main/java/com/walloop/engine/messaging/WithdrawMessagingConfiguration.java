package com.walloop.engine.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WithdrawMessagingConfiguration {

    public static final String CORE_WITHDRAW_QUEUE = "core.withdraw.queue";
    public static final String ENGINE_WITHDRAW_QUEUE = "engine.withdraw.queue";
    public static final String BALANCE_PROCESS_ROUTING_KEY = "balance.process";
    public static final String BALANCE_SENT_ROUTING_KEY = "balance.sent";

    @Bean
    public Queue coreWithdrawQueue() {
        return new Queue(CORE_WITHDRAW_QUEUE, true);
    }

    @Bean
    public Queue engineWithdrawQueue() {
        return new Queue(ENGINE_WITHDRAW_QUEUE, true);
    }

    @Bean
    public Binding balanceProcessBinding(
            @Qualifier("coreWithdrawQueue") Queue coreWithdrawQueue,
            @Qualifier("walloopCoreExchange") DirectExchange walloopCoreExchange
    ) {
        return BindingBuilder.bind(coreWithdrawQueue)
                .to(walloopCoreExchange)
                .with(BALANCE_PROCESS_ROUTING_KEY);
    }

    @Bean
    public Binding balanceSentBinding(
            @Qualifier("engineWithdrawQueue") Queue engineWithdrawQueue,
            @Qualifier("walloopEngineExchange") DirectExchange walloopEngineExchange
    ) {
        return BindingBuilder.bind(engineWithdrawQueue)
                .to(walloopEngineExchange)
                .with(BALANCE_SENT_ROUTING_KEY);
    }

    @Bean
    @Primary
    public RabbitTemplate withdrawRabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return template;
    }
}
