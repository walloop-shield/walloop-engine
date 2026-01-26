package com.walloop.engine.swap;

import java.util.UUID;

public record SwapToLiquidRequest(
        String depositCoin,
        String depositNetwork,
        String settleAddress,
        String refundAddress,
        UUID processId,
        String sessionToken
) {
}
