package com.walloop.engine.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerMessagingConfiguration {

    public static final String QUEUE_CREATED = "customer.created.queue";

    @Bean
    public TopicExchange customerExchange() {
        return ExchangeBuilder.topicExchange(CustomerProducer.EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue customerCreatedQueue() {
        return new Queue(QUEUE_CREATED, true);
    }

    @Bean
    public Binding customerCreatedBinding(TopicExchange customerExchange, Queue customerCreatedQueue) {
        return BindingBuilder.bind(customerCreatedQueue)
                .to(customerExchange)
                .with(CustomerProducer.ROUTING_KEY_CREATED);
    }
}
