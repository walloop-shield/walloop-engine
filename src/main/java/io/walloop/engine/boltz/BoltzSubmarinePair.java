package io.walloop.engine.boltz;

public record BoltzSubmarinePair(
        String hash,
        Double rate,
        BoltzSwapLimits limits,
        BoltzSwapFees fees
) {
}

