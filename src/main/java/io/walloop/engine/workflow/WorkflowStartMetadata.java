package io.walloop.engine.workflow;

import java.util.UUID;

public record WorkflowStartMetadata(
        UUID transactionId,
        UUID ownerId
) {
}


