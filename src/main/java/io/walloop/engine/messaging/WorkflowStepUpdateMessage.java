package io.walloop.engine.messaging;

import java.time.Instant;
import java.util.UUID;

public record WorkflowStepUpdateMessage(
        UUID executionId,
        UUID processId,
        UUID ownerId,
        String stepKey,
        String status,
        Instant executedAt
) {
}

