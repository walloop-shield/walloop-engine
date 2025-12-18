package com.walloop.engine.transaction.service;

import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.entity.WalletTransactionEntity;
import com.walloop.engine.transaction.repository.WalletTransactionRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletTransactionQueryServiceImpl implements WalletTransactionQueryService {

    private final WalletTransactionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<WalletTransactionDetails> find(UUID transactionId, UUID ownerId) {
        return repository.findByIdAndOwnerId(transactionId, ownerId).map(this::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletTransactionDetails require(UUID transactionId, UUID ownerId) {
        return find(transactionId, ownerId).orElseThrow(() -> new IllegalArgumentException(
                "Transaction not found for id=" + transactionId + " ownerId=" + ownerId));
    }

    private WalletTransactionDetails toDetails(WalletTransactionEntity entity) {
        return new WalletTransactionDetails(
                entity.getId(),
                entity.getOwnerId(),
                entity.getChain(),
                entity.getCorrelatedAddress(),
                entity.getNewAddress(),
                entity.getCreatedAt(),
                entity.getStatus()
        );
    }
}

