package io.walloop.engine.swap;

public record SwapToLiquidResult(
        String swapId,
        String depositAddress,
        String depositCoin,
        String depositNetwork,
        String settleCoin,
        String settleNetwork
) {
}

