package com.walloop.engine.boltz;

public record BoltzClaimSignature(
        String pubNonce,
        String partialSignature
) {
}
