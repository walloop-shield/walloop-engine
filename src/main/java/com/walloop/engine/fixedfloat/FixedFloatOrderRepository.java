package com.walloop.engine.fixedfloat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedFloatOrderRepository extends JpaRepository<FixedFloatOrderEntity, UUID> {
    Optional<FixedFloatOrderEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);

    List<FixedFloatOrderEntity> findByCompletedAtIsNull();

    boolean existsByCompletedAtIsNull();
}
