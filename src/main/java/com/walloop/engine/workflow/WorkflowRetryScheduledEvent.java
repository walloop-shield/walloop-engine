package com.walloop.engine.workflow;

import java.util.UUID;

public record WorkflowRetryScheduledEvent(UUID executionId) {
}
