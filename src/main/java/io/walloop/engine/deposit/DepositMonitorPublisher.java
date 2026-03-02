package io.walloop.engine.deposit;

import io.walloop.engine.dto.DepositMonitorMessage;
import io.walloop.engine.messaging.CoreMessagingConfiguration;
import io.walloop.engine.messaging.DepositMonitoringMessagingConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepositMonitorPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(DepositMonitorMessage message) {
        rabbitTemplate.convertAndSend(
                CoreMessagingConfiguration.CORE_EXCHANGE,
                DepositMonitoringMessagingConfiguration.MONITOR_WAITING_ROUTING_KEY,
                message
        );
    }
}

