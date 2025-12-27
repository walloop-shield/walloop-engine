package com.walloop.engine.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WithdrawMessagingConfiguration {

    public static final String WITHDRAW_EXCHANGE = "withdraw.exchange";
    public static final String WITHDRAW_QUEUE = "withdraw.queue";
    public static final String WITHDRAW_ROUTING_KEY = "withdraw.request";

    public static final String WITHDRAW_COMPLETED_QUEUE = "withdraw.completed.queue";
    public static final String WITHDRAW_COMPLETED_EXCHANGE = "withdraw.completed.exchange";
    public static final String WITHDRAW_COMPLETED_ROUTING_KEY = "withdraw.completed";

    @Bean
    public DirectExchange withdrawRequestExchange() {
        return new DirectExchange(WITHDRAW_EXCHANGE, true, false);
    }

    @Bean
    public Queue withdrawRequestQueue() {
        return new Queue(WITHDRAW_QUEUE, true);
    }

    @Bean
    public Binding withdrawRequestBinding(Queue withdrawRequestQueue, DirectExchange withdrawRequestExchange) {
        return BindingBuilder.bind(withdrawRequestQueue)
                .to(withdrawRequestExchange)
                .with(WITHDRAW_ROUTING_KEY);
    }

    @Bean
    public DirectExchange withdrawCompletedExchange() {
        return new DirectExchange(WITHDRAW_COMPLETED_EXCHANGE, true, false);
    }

    @Bean
    public Queue withdrawCompletedQueue() {
        return new Queue(WITHDRAW_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding withdrawCompletedBinding(Queue withdrawCompletedQueue, DirectExchange withdrawCompletedExchange) {
        return BindingBuilder.bind(withdrawCompletedQueue)
                .to(withdrawCompletedExchange)
                .with(WITHDRAW_COMPLETED_ROUTING_KEY);
    }

    @Bean
    @Primary
    public RabbitTemplate withdrawRabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return template;
    }
}
