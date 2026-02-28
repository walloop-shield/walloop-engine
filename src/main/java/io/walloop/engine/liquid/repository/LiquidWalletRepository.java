package io.walloop.engine.liquid.repository;

import io.walloop.engine.liquid.entity.LiquidWalletEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiquidWalletRepository extends JpaRepository<LiquidWalletEntity, UUID> {
    java.util.Optional<LiquidWalletEntity> findFirstByTransactionIdOrderByCreatedAtDesc(UUID transactionId);
}

