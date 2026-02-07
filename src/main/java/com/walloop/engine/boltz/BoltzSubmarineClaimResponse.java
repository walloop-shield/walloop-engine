package com.walloop.engine.boltz;

public record BoltzSubmarineClaimResponse(
        String preimage,
        String pubNonce,
        String transactionHash
) {
}
