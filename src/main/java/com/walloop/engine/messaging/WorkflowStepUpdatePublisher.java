package com.walloop.engine.messaging;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowStepUpdatePublisher {

    public static final String ROUTING_KEY = "engine.workflow.step";

    private final RabbitTemplate engineTimelineRabbitTemplate;

    public void publish(UUID executionId, UUID processId, UUID ownerId, String stepKey, String status, Instant executedAt) {
        if (executionId == null || processId == null || ownerId == null) {
            return;
        }
        WorkflowStepUpdateMessage message = new WorkflowStepUpdateMessage(
                executionId,
                processId,
                ownerId,
                stepKey,
                status,
                executedAt
        );
        engineTimelineRabbitTemplate.convertAndSend(
                EngineMessagingConfiguration.ENGINE_EXCHANGE,
                ROUTING_KEY,
                message
        );
    }
}
