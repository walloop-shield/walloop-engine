package com.walloop.engine.lightning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LightningInboundLiquidityRequestRepository
        extends JpaRepository<LightningInboundLiquidityRequestEntity, UUID> {

    Optional<LightningInboundLiquidityRequestEntity> findFirstByProcessIdAndStatusInOrderByCreatedAtDesc(
            UUID processId,
            List<LightningInboundLiquidityRequestStatus> statuses
    );
}
