package io.walloop.engine.lightning;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboundLiquidityInvoiceResponse(
        UUID requestId,
        String invoice,
        long amountSats,
        String targetNodePubkey,
        long targetChannelId,
        long targetChannelCapacitySats,
        long targetChannelLocalBalanceSats,
        long targetChannelRemoteBalanceSats,
        long targetChannelSpendableSats,
        String status,
        OffsetDateTime createdAt
) {
}

