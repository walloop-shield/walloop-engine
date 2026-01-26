package com.walloop.engine.swap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapOrderRepository extends JpaRepository<SwapOrderEntity, UUID> {
    Optional<SwapOrderEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);

    List<SwapOrderEntity> findByStatusIsNullOrStatusNot(SwapOrderStatus status);

    boolean existsByStatusIsNullOrStatusNot(SwapOrderStatus status);
}
