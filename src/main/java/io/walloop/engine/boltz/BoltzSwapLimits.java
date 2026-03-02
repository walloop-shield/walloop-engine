package io.walloop.engine.boltz;

public record BoltzSwapLimits(
        Long maximal,
        Long minimal,
        Long maximalZeroConf
) {
}

