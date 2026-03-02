package io.walloop.engine.workflow;

import java.time.Duration;

public record StepResult(
        StepStatus status,
        String detail,
        Duration retryAfter
) {
    public static StepResult completed(String detail) {
        return new StepResult(StepStatus.COMPLETED, detail, null);
    }

    public static StepResult waiting(String detail) {
        return new StepResult(StepStatus.WAITING, detail, null);
    }

    public static StepResult retry(String detail, Duration retryAfter) {
        return new StepResult(StepStatus.RETRY, detail, retryAfter);
    }

    public static StepResult failed(String detail) {
        return new StepResult(StepStatus.FAILED, detail, null);
    }
}


