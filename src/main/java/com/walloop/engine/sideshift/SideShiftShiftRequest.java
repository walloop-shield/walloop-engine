package com.walloop.engine.sideshift;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SideShiftShiftRequest {
    String depositCoin;
    String depositNetwork;
    String settleCoin;
    String settleNetwork;
    String settleAddress;
}

