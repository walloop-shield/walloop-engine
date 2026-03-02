package io.walloop.engine.liquid.service;

import io.walloop.engine.liquid.entity.LiquidWalletEntity;
import io.walloop.engine.liquid.repository.LiquidWalletRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiquidWalletService {

    private final LiquidRpcService rpcService;
    private final LiquidWalletRepository repository;

    @Transactional
    public LiquidWalletEntity createForTransaction(UUID transactionId, UUID ownerId) {
        Optional<LiquidWalletEntity> existing = repository.findFirstByTransactionIdOrderByCreatedAtDesc(transactionId);
        if (existing.isPresent()) {
            return existing.get();
        }
        String label = "tx-" + transactionId;
        String address = rpcService.getNewAddress(label);
        String privateKey = rpcService.dumpPrivateKey(address);

        LiquidWalletEntity entity = new LiquidWalletEntity();
        entity.setTransactionId(transactionId);
        entity.setOwnerId(ownerId);
        entity.setAddress(address);
        entity.setPrivateKey(privateKey);
        entity.setLabel(label);
        entity.setCreatedAt(OffsetDateTime.now());

        LiquidWalletEntity saved = repository.save(entity);
        log.info("LiquidWalletService - liquid wallet created tx={} owner={} address={}", transactionId, ownerId, address);
        return saved;
    }
}


