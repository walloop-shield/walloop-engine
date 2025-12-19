package com.walloop.engine.sideshift;

public record SideShiftShiftResponse(
        String id,
        String depositAddress,
        String depositCoin,
        String depositNetwork,
        String settleCoin,
        String settleNetwork
) {
}

