package com.walloop.engine.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DepositMonitoringMessagingConfiguration {

    public static final String CORE_DEPOSIT_QUEUE = "core.deposit.queue";
    public static final String ENGINE_DEPOSIT_QUEUE = "engine.deposit.queue";
    public static final String ENGINE_DEPOSIT_DLQ = "engine.deposit.dlq";
    public static final String MONITOR_WAITING_ROUTING_KEY = "monitor.waiting";
    public static final String MONITOR_DETECTED_ROUTING_KEY = "monitor.detected";
    public static final String MONITOR_DETECTED_DLQ_ROUTING_KEY = "monitor.detected.dlq";

    @Bean
    public Queue coreDepositQueue() {
        return new Queue(CORE_DEPOSIT_QUEUE, true);
    }

    @Bean
    public Queue engineDepositQueue() {
        return new Queue(ENGINE_DEPOSIT_QUEUE, true, false, false, deadLetterArgs(MONITOR_DETECTED_DLQ_ROUTING_KEY));
    }

    @Bean
    public Queue engineDepositDlq() {
        return new Queue(ENGINE_DEPOSIT_DLQ, true);
    }

    @Bean
    public Binding monitorWaitingBinding(
            @Qualifier("coreDepositQueue") Queue coreDepositQueue,
            @Qualifier("walloopCoreExchange") DirectExchange walloopCoreExchange
    ) {
        return BindingBuilder.bind(coreDepositQueue)
                .to(walloopCoreExchange)
                .with(MONITOR_WAITING_ROUTING_KEY);
    }

    @Bean
    public Binding monitorDetectedBinding(
            @Qualifier("engineDepositQueue") Queue engineDepositQueue,
            @Qualifier("walloopEngineExchange") DirectExchange walloopEngineExchange
    ) {
        return BindingBuilder.bind(engineDepositQueue)
                .to(walloopEngineExchange)
                .with(MONITOR_DETECTED_ROUTING_KEY);
    }

    @Bean
    public Binding monitorDetectedDlqBinding(
            @Qualifier("engineDepositDlq") Queue engineDepositDlq,
            @Qualifier("walloopEngineDeadLetterExchange") DirectExchange walloopEngineDeadLetterExchange
    ) {
        return BindingBuilder.bind(engineDepositDlq)
                .to(walloopEngineDeadLetterExchange)
                .with(MONITOR_DETECTED_DLQ_ROUTING_KEY);
    }

    private Map<String, Object> deadLetterArgs(String routingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EngineMessagingConfiguration.ENGINE_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", routingKey);
        return args;
    }
}
