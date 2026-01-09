package com.walloop.engine.fee;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeCalculationRepository extends JpaRepository<FeeCalculationEntity, UUID> {

    Optional<FeeCalculationEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);
}
