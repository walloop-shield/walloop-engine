package io.walloop.engine.lightning;

public record LspLiquidityResponse(
        String externalId,
        String responsePayload,
        String nodeAddress
) {
}

