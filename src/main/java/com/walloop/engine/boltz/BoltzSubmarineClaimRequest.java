package com.walloop.engine.boltz;

public record BoltzSubmarineClaimRequest(
        String pubNonce,
        String partialSignature
) {
}
