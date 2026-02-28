package io.walloop.engine.messaging;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WithdrawRequestPublisher {

    private static final String DESTINATION_TO_LIQUID_NETWORK = "TO_LIQUID_NETWORK";
    public static final String DESTINATION_TO_PRINCIPAL_WALLET = "TO_PRINCIPAL_WALLET";

    private final RabbitTemplate withdrawRabbitTemplate;

    public void publish(UUID processId) {
        publish(processId, DESTINATION_TO_LIQUID_NETWORK);
    }

    public void publish(UUID processId, String destination) {
        WithdrawRequestMessage message = new WithdrawRequestMessage(processId, destination);
        withdrawRabbitTemplate.convertAndSend(
                CoreMessagingConfiguration.CORE_EXCHANGE,
                WithdrawMessagingConfiguration.BALANCE_PROCESS_ROUTING_KEY,
                message
        );
    }
}

