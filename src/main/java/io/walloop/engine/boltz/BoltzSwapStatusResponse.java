package io.walloop.engine.boltz;

public record BoltzSwapStatusResponse(
        String status,
        BoltzSwapTransaction transaction
) {
}

