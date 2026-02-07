package com.walloop.engine.boltz;

public record BoltzSubmarineResponse(
        String id,
        String bip21,
        String address,
        Long expectedAmount,
        Boolean acceptZeroConf,
        String claimPublicKey,
        String swapTree
) {
}
