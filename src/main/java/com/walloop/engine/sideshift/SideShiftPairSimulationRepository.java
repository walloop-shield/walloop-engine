package com.walloop.engine.sideshift;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SideShiftPairSimulationRepository extends JpaRepository<SideShiftPairSimulationEntity, UUID> {
    Optional<SideShiftPairSimulationEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);
}
