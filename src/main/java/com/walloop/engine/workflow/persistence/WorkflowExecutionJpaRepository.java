package com.walloop.engine.workflow.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionJpaRepository extends JpaRepository<WorkflowExecutionEntity, UUID> {

    @EntityGraph(attributePaths = "steps")
    java.util.Optional<WorkflowExecutionEntity> findById(UUID id);
}
