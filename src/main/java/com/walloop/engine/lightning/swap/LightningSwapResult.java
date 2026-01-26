package com.walloop.engine.lightning.swap;

public record LightningSwapResult(
        String swapId,
        String lockupAddress,
        Long expectedAmount,
        String requestPayload,
        String responsePayload
) {
}
