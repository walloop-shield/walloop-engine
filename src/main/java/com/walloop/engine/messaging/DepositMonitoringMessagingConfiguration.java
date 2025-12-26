package com.walloop.engine.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DepositMonitoringMessagingConfiguration {

    public static final String DEPOSIT_MONITOR_QUEUE = "deposit.monitor.queue";
    public static final String DEPOSIT_DETECTED_QUEUE = "deposit.detected.queue";

    @Bean
    public Queue depositMonitorQueue() {
        return new Queue(DEPOSIT_MONITOR_QUEUE, true);
    }

    @Bean
    public Queue depositDetectedQueue() {
        return new Queue(DEPOSIT_DETECTED_QUEUE, true);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return template;
    }
}
