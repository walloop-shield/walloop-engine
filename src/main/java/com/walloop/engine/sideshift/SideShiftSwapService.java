package com.walloop.engine.sideshift;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SideShiftSwapService {

    private final SideShiftClient client;

    public SideShiftShiftResponse swapToLiquidUsdt(String depositCoin, String depositNetwork, String settleAddress) {
        return client.createShift(depositCoin, depositNetwork, settleAddress);
    }
}

