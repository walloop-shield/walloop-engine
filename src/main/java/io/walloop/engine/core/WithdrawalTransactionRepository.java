package io.walloop.engine.core;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WithdrawalTransactionRepository extends JpaRepository<WithdrawalTransactionEntity, UUID> {

    @Query("select coalesce(sum(w.feeBaseUnit), 0) from WithdrawalTransactionEntity w "
        + "where w.processId = :processId and lower(w.chain) in :chains")
    BigInteger sumFeeBaseUnitByProcessId(@Param("processId") UUID processId, @Param("chains") Collection<String> chains);

    List<WithdrawalTransactionEntity> findByProcessIdOrderByCreatedAtAsc(UUID processId);

    Optional<WithdrawalTransactionEntity> findFirstByProcessIdAndDestinationOrderByCreatedAtDesc(
            UUID processId,
            String destination
    );
}

