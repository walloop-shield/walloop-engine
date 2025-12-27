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
    public Optional<WalletTransactionDetails> find(UUID id, UUID ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId).map(this::toDetails);
    }

    @Override
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
