package com.walloop.engine.transaction.service;

import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionQueryService {

    Optional<WalletTransactionDetails> find(UUID transactionId, UUID ownerId);

    WalletTransactionDetails require(UUID transactionId, UUID ownerId);
}

