package com.walloop.engine.conversion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversionOrderRepository extends JpaRepository<ConversionOrderEntity, UUID> {
    Optional<ConversionOrderEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);

    Optional<ConversionOrderEntity> findFirstByProcessIdAndPartnerOrderByCreatedAtDesc(
            UUID processId,
            ConversionPartner partner
    );

    List<ConversionOrderEntity> findByCompletedAtIsNull();
}
