package io.walloop.engine.lightning;

import java.util.UUID;

public record LspLiquidityRequest(
        UUID processId,
        String nodePubKey,
        long targetInboundSats,
        long currentInboundSats,
        long requestedSats
) {
}

