package io.walloop.engine.transaction.repository;

import io.walloop.engine.transaction.entity.WalletTransactionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransactionEntity, UUID> {

    Optional<WalletTransactionEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}


