package com.walloop.engine.workflow;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowExecutionRepository {

    WorkflowExecution save(WorkflowExecution execution);

    Optional<WorkflowExecution> findById(UUID executionId);

    Optional<WorkflowExecution> findByTransactionId(UUID transactionId);
}

