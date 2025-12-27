package com.walloop.engine.transaction.service;

import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionQueryService {

    Optional<WalletTransactionDetails> find(UUID id, UUID ownerId);

    WalletTransactionDetails require(UUID id, UUID ownerId);
}
