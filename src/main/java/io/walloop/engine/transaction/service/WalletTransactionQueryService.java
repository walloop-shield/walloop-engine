package io.walloop.engine.transaction.service;

import io.walloop.engine.transaction.dto.WalletTransactionDetails;
import io.walloop.engine.transaction.entity.WalletTransactionEntity;
import io.walloop.engine.transaction.repository.WalletTransactionRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletTransactionQueryService {

    private final WalletTransactionRepository repository;

    @Transactional(readOnly = true)
    public Optional<WalletTransactionDetails> find(UUID id, UUID ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId).map(this::toDetails);
    }

    @Transactional(readOnly = true)
    public WalletTransactionDetails require(UUID id, UUID ownerId) {
        return find(id, ownerId).orElseThrow(() -> new IllegalArgumentException(
                "Transaction not found for id=" + id + " ownerId=" + ownerId));
    }

    private WalletTransactionDetails toDetails(WalletTransactionEntity entity) {
        return new WalletTransactionDetails(
                entity.getId(),
                entity.getOwnerId(),
                entity.getChain(),
                entity.getCorrelatedAddress(),
                entity.getNewAddress(),
                entity.getNewAddress2(),
                entity.getCreatedAt(),
                entity.getStatus()
        );
    }
}

