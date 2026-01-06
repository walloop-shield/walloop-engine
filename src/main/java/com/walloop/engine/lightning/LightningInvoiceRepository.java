package com.walloop.engine.lightning;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LightningInvoiceRepository extends JpaRepository<LightningInvoiceEntity, UUID> {
    Optional<LightningInvoiceEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);

    java.util.List<LightningInvoiceEntity> findByBoltzSwapIdIsNotNullAndStatusNot(LightningInvoiceStatus status);

    boolean existsByBoltzSwapIdIsNotNullAndStatusNot(LightningInvoiceStatus status);
}
