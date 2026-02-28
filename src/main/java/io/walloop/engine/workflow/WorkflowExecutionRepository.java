package io.walloop.engine.workflow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowExecutionRepository {

    WorkflowExecution save(WorkflowExecution execution);

    Optional<WorkflowExecution> findById(UUID executionId);

    Optional<WorkflowExecution> findByTransactionId(UUID transactionId);

    boolean existsPendingRetries();

    List<WorkflowExecution> findRetriesDue(Instant now);
}


