package com.walloop.engine.workflow;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWorkflowExecutionRepository implements WorkflowExecutionRepository {

    private final Map<UUID, WorkflowExecution> storage = new ConcurrentHashMap<>();

    @Override
    public WorkflowExecution save(WorkflowExecution execution) {
        storage.put(execution.getId(), execution);
        return execution;
    }

    @Override
    public Optional<WorkflowExecution> findById(UUID executionId) {
        return Optional.ofNullable(storage.get(executionId));
    }
}
