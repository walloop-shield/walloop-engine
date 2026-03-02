package io.walloop.engine.lightning;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LightningOutboundLiquidityRequestRepository
        extends JpaRepository<LightningOutboundLiquidityRequestEntity, UUID> {

    boolean existsByStatusIn(Collection<LightningOutboundLiquidityRequestStatus> statuses);

    List<LightningOutboundLiquidityRequestEntity> findByStatusIn(Collection<LightningOutboundLiquidityRequestStatus> statuses);
}

