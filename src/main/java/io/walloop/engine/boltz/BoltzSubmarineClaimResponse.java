package io.walloop.engine.boltz;

public record BoltzSubmarineClaimResponse(
        String preimage,
        String pubNonce,
        String transactionHash
) {
}

