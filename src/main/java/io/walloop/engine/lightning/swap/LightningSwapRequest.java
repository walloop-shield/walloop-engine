package io.walloop.engine.lightning.swap;

public record LightningSwapRequest(
        String fromAsset,
        String toAsset,
        String invoice,
        String refundPublicKey
) {
}

