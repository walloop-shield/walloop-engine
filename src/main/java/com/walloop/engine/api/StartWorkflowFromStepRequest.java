package com.walloop.engine.api;

import java.util.UUID;

public record StartWorkflowFromStepRequest(UUID processId, UUID ownerId, String stepKey) {
}
