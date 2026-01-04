package com.walloop.engine.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DepositMonitoringMessagingConfiguration {

    public static final String CORE_DEPOSIT_QUEUE = "core.deposit.queue";
    public static final String ENGINE_DEPOSIT_QUEUE = "engine.deposit.queue";
    public static final String MONITOR_WAITING_ROUTING_KEY = "monitor.waiting";
    public static final String MONITOR_DETECTED_ROUTING_KEY = "monitor.detected";

    @Bean
    public Queue coreDepositQueue() {
        return new Queue(CORE_DEPOSIT_QUEUE, true);
    }

    @Bean
    public Queue engineDepositQueue() {
        return new Queue(ENGINE_DEPOSIT_QUEUE, true);
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

}
