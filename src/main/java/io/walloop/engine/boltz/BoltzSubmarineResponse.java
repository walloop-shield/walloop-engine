package io.walloop.engine.boltz;

import java.util.Map;

public record BoltzSubmarineResponse(
        String id,
        String bip21,
        String address,
        Long expectedAmount,
        Boolean acceptZeroConf,
        String claimPublicKey,
        Map<String, Object> swapTree
) {
}

