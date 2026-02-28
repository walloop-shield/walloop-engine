package io.walloop.engine.withdrawal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalloopWithdrawalRepository extends JpaRepository<WalloopWithdrawalEntity, UUID> {
    Optional<WalloopWithdrawalEntity> findFirstByProcessIdOrderByCreatedAtDesc(UUID processId);
}

