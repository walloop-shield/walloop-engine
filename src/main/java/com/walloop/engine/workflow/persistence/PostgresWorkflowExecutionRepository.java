package com.walloop.engine.workflow.persistence;

import com.walloop.engine.workflow.StepExecution;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class PostgresWorkflowExecutionRepository implements WorkflowExecutionRepository {

    private final WorkflowExecutionJpaRepository jpaRepository;

    @Override
    @Transactional
    public WorkflowExecution save(WorkflowExecution execution) {
        WorkflowExecutionEntity entity = jpaRepository.findById(execution.getId())
                .orElseGet(() -> {
                    WorkflowExecutionEntity created = new WorkflowExecutionEntity();
                    created.setId(execution.getId());
                    created.setCreatedAt(execution.getCreatedAt());
                    return created;
                });

        entity.setWorkflowName(execution.getWorkflowName());
        entity.setTransactionId(execution.getTransactionId());
        entity.setOwnerId(execution.getOwnerId());
        entity.setStatus(execution.getStatus());
        entity.setNextStepIndex(execution.getNextStepIndex());
        entity.setUpdatedAt(execution.getUpdatedAt());

        entity.getSteps().clear();
        int stepIndex = 0;
        for (StepExecution historyItem : execution.getHistory()) {
            WorkflowStepExecutionEntity stepEntity = new WorkflowStepExecutionEntity();
            stepEntity.setExecution(entity);
            stepEntity.setStepIndex(stepIndex++);
            stepEntity.setStepKey(historyItem.stepKey());
            stepEntity.setStatus(historyItem.status());
            stepEntity.setDetail(historyItem.detail());
            stepEntity.setExecutedAt(historyItem.executedAt());
            entity.getSteps().add(stepEntity);
        }

        WorkflowExecutionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowExecution> findById(UUID executionId) {
        return jpaRepository.findById(executionId).map(this::toDomain);
    }

    private WorkflowExecution toDomain(WorkflowExecutionEntity entity) {
        WorkflowExecution restored = WorkflowExecution.restore(
                entity.getId(),
                entity.getWorkflowName(),
                entity.getTransactionId(),
                entity.getOwnerId(),
                entity.getStatus(),
                entity.getNextStepIndex(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );

        entity.getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStepExecutionEntity::getStepIndex))
                .forEach(step -> restored.addHistoryNoTouch(new StepExecution(
                        step.getStepKey(),
                        step.getStatus(),
                        step.getDetail(),
                        step.getExecutedAt()
                )));

        return restored;
    }
}
