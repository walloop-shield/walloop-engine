package com.walloop.engine.api;

import java.util.UUID;

public record StartWorkflowFromStepResponse(
        UUID executionId,
        String workflowName,
        String status,
        int nextStepIndex,
        String stepKey
) {
}
