package io.walloop.engine.messaging;

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
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class TransactionEngineMessagingConfiguration {

    public static final String WALLOOP_ENGINE_EXCHANGE = "walloop.engine.exchange";
    public static final String WALLOOP_ENGINE_DEAD_LETTER_EXCHANGE = "walloop.engine.dlx";
    public static final String TRANSACTION_ENGINE_QUEUE = "transaction.engine.queue";
    public static final String TRANSACTION_ENGINE_DLQ = "transaction.engine.dlq";
    public static final String ENGINE_INITIALIZATION_ROUTING_KEY = "engine.initialization";
    public static final String ENGINE_INITIALIZATION_DLQ_ROUTING_KEY = "engine.initialization.dlq";
    public static final String TRANSACTION_ENGINE_LISTENER_CONTAINER_FACTORY = "transactionEngineListenerContainerFactory";

    @Bean
    public DirectExchange transactionEngineExchange() {
        return new DirectExchange(WALLOOP_ENGINE_EXCHANGE, true, false);
    }

    @Bean
    public Queue transactionEngineQueue() {
        return new Queue(TRANSACTION_ENGINE_QUEUE, true, false, false, deadLetterArgs(ENGINE_INITIALIZATION_DLQ_ROUTING_KEY));
    }

    @Bean
    public Queue transactionEngineDlq() {
        return new Queue(TRANSACTION_ENGINE_DLQ, true);
    }

    @Bean
    public Binding transactionEngineBinding(
            @Qualifier("transactionEngineQueue") Queue transactionEngineQueue,
            @Qualifier("transactionEngineExchange") DirectExchange transactionEngineExchange
    ) {
        return BindingBuilder.bind(transactionEngineQueue)
                .to(transactionEngineExchange)
                .with(ENGINE_INITIALIZATION_ROUTING_KEY);
    }

    @Bean
    public Binding transactionEngineDlqBinding(
            @Qualifier("transactionEngineDlq") Queue transactionEngineDlq,
            @Qualifier("walloopEngineDeadLetterExchange") DirectExchange walloopEngineDeadLetterExchange
    ) {
        return BindingBuilder.bind(transactionEngineDlq)
                .to(walloopEngineDeadLetterExchange)
                .with(ENGINE_INITIALIZATION_DLQ_ROUTING_KEY);
    }

    @Bean(TRANSACTION_ENGINE_LISTENER_CONTAINER_FACTORY)
    public RabbitListenerContainerFactory<?> transactionEngineListenerContainerFactory(
            ConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            SimpleRabbitListenerContainerFactoryConfigurer configurer
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return factory;
    }

    private Map<String, Object> deadLetterArgs(String routingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", WALLOOP_ENGINE_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", routingKey);
        return args;
    }
}

