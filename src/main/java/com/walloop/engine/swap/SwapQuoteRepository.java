package com.walloop.engine.swap;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapQuoteRepository extends JpaRepository<SwapQuoteEntity, UUID> {
    Optional<SwapQuoteEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);

    Optional<SwapQuoteEntity> findFirstByProcessIdAndPartnerOrderByCreatedAtDesc(UUID processId, SwapPartner partner);
}
