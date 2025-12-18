package com.walloop.engine.workflow;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SequentialWorkflowOrchestrator implements WorkflowOrchestrator {

    private final WorkflowExecutionRepository repository;

    @Override
    public WorkflowExecution start(WorkflowDefinition definition, WorkflowContext context, WorkflowStartMetadata metadata) {
        WorkflowExecution execution = new WorkflowExecution(UUID.randomUUID(), definition.name());
        if (metadata != null) {
            execution.setTransactionId(metadata.transactionId());
            execution.setOwnerId(metadata.ownerId());
        }
        repository.save(execution);
        return run(definition, execution, context);
    }

    @Override
    public WorkflowExecution resume(UUID executionId, WorkflowDefinition definition, WorkflowContext context) {
        WorkflowExecution execution = repository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow execution not found: " + executionId));
        return run(definition, execution, context);
    }

    private WorkflowExecution run(WorkflowDefinition definition, WorkflowExecution execution, WorkflowContext context) {
        if (!definition.name().equals(execution.getWorkflowName())) {
            throw new IllegalArgumentException(
                    "Workflow mismatch. execution=" + execution.getWorkflowName() + " definition=" + definition.name());
        }

        if (execution.getStatus() == WorkflowStatus.COMPLETED || execution.getStatus() == WorkflowStatus.FAILED) {
            return execution;
        }

        execution.setStatus(WorkflowStatus.RUNNING);
        repository.save(execution);

        while (execution.getNextStepIndex() < definition.steps().size()) {
            WorkflowStep step = definition.steps().get(execution.getNextStepIndex());
            try {
                StepResult result = step.execute(context);
                execution.addHistory(new StepExecution(step.key(), result.status(), result.detail(), Instant.now()));

                if (result.status() == StepStatus.COMPLETED) {
                    execution.setNextStepIndex(execution.getNextStepIndex() + 1);
                    repository.save(execution);
                    continue;
                }

                if (result.status() == StepStatus.WAITING || result.status() == StepStatus.RETRY) {
                    execution.setStatus(WorkflowStatus.WAITING);
                    repository.save(execution);
                    log.info("Workflow {} waiting at step {}: {}", execution.getId(), step.key(), result.detail());
                    return execution;
                }

                execution.setStatus(WorkflowStatus.FAILED);
                repository.save(execution);
                log.warn("Workflow {} failed at step {}: {}", execution.getId(), step.key(), result.detail());
                return execution;
            } catch (Exception e) {
                execution.addHistory(new StepExecution(step.key(), StepStatus.FAILED, e.getMessage(), Instant.now()));
                execution.setStatus(WorkflowStatus.FAILED);
                repository.save(execution);
                log.error("Workflow {} exception at step {}", execution.getId(), step.key(), e);
                return execution;
            }
        }

        execution.setStatus(WorkflowStatus.COMPLETED);
        repository.save(execution);
        return execution;
    }
}
