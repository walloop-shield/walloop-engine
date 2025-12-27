package com.walloop.engine.messaging;

import org.springframework.amqp.core.Queue;
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

}
