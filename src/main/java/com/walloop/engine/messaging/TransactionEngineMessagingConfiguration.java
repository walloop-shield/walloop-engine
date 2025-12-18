package com.walloop.engine.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionEngineMessagingConfiguration {

    public static final String TX_ENGINE_EXCHANGE = "transaction.engine.exchange";
    public static final String TX_ENGINE_QUEUE = "transaction.engine.queue";
    public static final String TX_ENGINE_ROUTING_KEY = "transaction.engine.start";
    public static final String TX_ENGINE_LISTENER_CONTAINER_FACTORY = "transactionEngineListenerContainerFactory";

    @Bean
    public DirectExchange transactionEngineExchange() {
        return new DirectExchange(TX_ENGINE_EXCHANGE, true, false);
    }

    @Bean
    public Queue transactionEngineQueue() {
        return new Queue(TX_ENGINE_QUEUE, true);
    }

    @Bean
    public Binding transactionEngineBinding(
            @Qualifier("transactionEngineQueue") Queue transactionEngineQueue,
            @Qualifier("transactionEngineExchange") DirectExchange transactionEngineExchange
    ) {
        return BindingBuilder.bind(transactionEngineQueue)
                .to(transactionEngineExchange)
                .with(TX_ENGINE_ROUTING_KEY);
    }

    @Bean(TX_ENGINE_LISTENER_CONTAINER_FACTORY)
    public RabbitListenerContainerFactory<?> transactionEngineListenerContainerFactory(
            ConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            RabbitProperties rabbitProperties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        factory.setAutoStartup(rabbitProperties.getListener().getSimple().isAutoStartup());
        return factory;
    }
}
