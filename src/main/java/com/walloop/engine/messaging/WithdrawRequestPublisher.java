package com.walloop.engine.messaging;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WithdrawRequestPublisher {

    private static final String DESTINATION_SIDESHIFT = "SIDESHIFT";
    public static final String DESTINATION_WALLOOP = "WALLOOP";

    private final RabbitTemplate withdrawRabbitTemplate;

    public void publish(UUID processId) {
        publish(processId, DESTINATION_SIDESHIFT);
    }

    public void publish(UUID processId, String destination) {
        WithdrawRequestMessage message = new WithdrawRequestMessage(processId, destination);
        withdrawRabbitTemplate.convertAndSend(
                WithdrawMessagingConfiguration.WITHDRAW_EXCHANGE,
                WithdrawMessagingConfiguration.WITHDRAW_ROUTING_KEY,
                message
        );
    }
}
