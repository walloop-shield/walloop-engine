package io.walloop.engine.boltz;

public record BoltzSubmarineClaimRequest(
        String pubNonce,
        String partialSignature
) {
}

