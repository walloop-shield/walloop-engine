package io.walloop.engine.lightning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LightningInboundLiquidityRequestRepository
        extends JpaRepository<LightningInboundLiquidityRequestEntity, UUID> {

    Optional<LightningInboundLiquidityRequestEntity> findFirstByProviderAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
            String provider,
            List<LightningInboundLiquidityRequestStatus> statuses,
            java.time.OffsetDateTime createdAt
    );

    Optional<LightningInboundLiquidityRequestEntity> findFirstByProviderAndStatusInOrderByCreatedAtDesc(
            String provider,
            List<LightningInboundLiquidityRequestStatus> statuses
    );

}

