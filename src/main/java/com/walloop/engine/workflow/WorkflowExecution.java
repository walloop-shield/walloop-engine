package com.walloop.engine.workflow;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkflowExecution {

    private final UUID id;
    private final String workflowName;
    private UUID transactionId;
    private UUID ownerId;
    private WorkflowStatus status;
    private int nextStepIndex;
    private Instant nextRetryAt;
    private int retryCount;
    private final List<StepExecution> history;
    private Instant createdAt;
    private Instant updatedAt;

    public WorkflowExecution(UUID id, String workflowName) {
        this.id = id;
        this.workflowName = workflowName;
        this.transactionId = null;
        this.ownerId = null;
        this.status = WorkflowStatus.RUNNING;
        this.nextStepIndex = 0;
        this.nextRetryAt = null;
        this.retryCount = 0;
        this.history = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static WorkflowExecution restore(
            UUID id,
            String workflowName,
            UUID transactionId,
            UUID ownerId,
            WorkflowStatus status,
            int nextStepIndex,
            Instant nextRetryAt,
            int retryCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        WorkflowExecution execution = new WorkflowExecution(id, workflowName);
        execution.transactionId = transactionId;
        execution.ownerId = ownerId;
        execution.status = status;
        execution.nextStepIndex = nextStepIndex;
        execution.nextRetryAt = nextRetryAt;
        execution.retryCount = retryCount;
        execution.createdAt = createdAt;
        execution.updatedAt = updatedAt;
        execution.history.clear();
        return execution;
    }

    public UUID getId() {
        return id;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
        touch();
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        touch();
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
        touch();
    }

    public int getNextStepIndex() {
        return nextStepIndex;
    }

    public void setNextStepIndex(int nextStepIndex) {
        this.nextStepIndex = nextStepIndex;
        touch();
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void scheduleRetry(Duration delay) {
        if (delay == null) {
            throw new IllegalArgumentException("retry delay is required");
        }
        this.nextRetryAt = Instant.now().plus(delay);
        this.retryCount += 1;
        touch();
    }

    public void clearRetry() {
        if (this.nextRetryAt != null) {
            this.nextRetryAt = null;
            touch();
        }
    }

    public List<StepExecution> getHistory() {
        return List.copyOf(history);
    }

    public void addHistory(StepExecution stepExecution) {
        this.history.add(stepExecution);
        touch();
    }

    public void addHistoryNoTouch(StepExecution stepExecution) {
        this.history.add(stepExecution);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
