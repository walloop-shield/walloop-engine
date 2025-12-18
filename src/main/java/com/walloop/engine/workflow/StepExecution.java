package com.walloop.engine.workflow;

import java.time.Instant;

public record StepExecution(
        String stepKey,
        StepStatus status,
        String detail,
        Instant executedAt
) {
}

