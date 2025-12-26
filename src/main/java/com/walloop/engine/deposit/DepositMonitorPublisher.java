package com.walloop.engine.deposit;

import com.walloop.engine.dto.DepositMonitorMessage;
import com.walloop.engine.messaging.DepositMonitoringMessagingConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepositMonitorPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(DepositMonitorMessage message) {
        rabbitTemplate.convertAndSend(DepositMonitoringMessagingConfiguration.DEPOSIT_MONITOR_QUEUE, message);
    }
}
