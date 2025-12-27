package com.walloop.engine.sideshift;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SideShiftShiftRepository extends JpaRepository<SideShiftShiftEntity, UUID> {
    Optional<SideShiftShiftEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);

    List<SideShiftShiftEntity> findByStatusIsNullOrStatusNot(SideShiftShiftStatus status);
}
