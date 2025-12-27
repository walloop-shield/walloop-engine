package com.walloop.engine.transaction.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WalletTransactionDetails(
        UUID id,
        UUID ownerId,
        String chain,
        String correlatedAddress,
        String newAddress,
        String newAddress2,
        OffsetDateTime createdAt,
        String status
) {
}
