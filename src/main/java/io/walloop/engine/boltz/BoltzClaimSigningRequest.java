package io.walloop.engine.boltz;

public record BoltzClaimSigningRequest(
        String swapId,
        String claimPublicKey,
        String swapTree,
        String pubNonce,
        String transactionHash,
        String refundPrivateKey
) {
}

