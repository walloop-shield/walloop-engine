package com.walloop.engine.workflow.persistence;

import com.walloop.engine.workflow.WorkflowStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionJpaRepository extends JpaRepository<WorkflowExecutionEntity, UUID> {

    @EntityGraph(attributePaths = "steps")
    java.util.Optional<WorkflowExecutionEntity> findById(UUID id);

    @EntityGraph(attributePaths = "steps")
    java.util.Optional<WorkflowExecutionEntity> findByTransactionId(UUID transactionId);

    boolean existsByStatusAndNextRetryAtIsNotNull(WorkflowStatus status);

    @EntityGraph(attributePaths = "steps")
    List<WorkflowExecutionEntity> findByStatusAndNextRetryAtLessThanEqual(WorkflowStatus status, Instant nextRetryAt);
}
