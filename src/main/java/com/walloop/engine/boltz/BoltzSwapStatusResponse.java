package com.walloop.engine.boltz;

public record BoltzSwapStatusResponse(
        String status,
        BoltzSwapTransaction transaction
) {
}
