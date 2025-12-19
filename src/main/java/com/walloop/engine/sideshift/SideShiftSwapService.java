package com.walloop.engine.sideshift;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SideShiftSwapService {

    private final SideShiftClient client;
    private final SideShiftProperties properties;

    public SideShiftShiftResponse swapToLiquidUsdt(String depositCoin, String depositNetwork, String settleAddress) {
        SideShiftShiftRequest request = SideShiftShiftRequest.builder()
                .depositCoin(depositCoin.toLowerCase())
                .depositNetwork(depositNetwork.toLowerCase())
                .settleCoin(properties.getSettleCoin())
                .settleNetwork(properties.getSettleNetwork())
                .settleAddress(settleAddress)
                .build();
        return client.createShift(request);
    }
}
