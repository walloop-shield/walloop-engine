package com.walloop.engine.swap;

import com.walloop.engine.sideshift.SideShiftShiftResponse;
import com.walloop.engine.sideshift.SideShiftSwapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SideShiftSwapPartner implements SwapToLiquidPartner {

    private final SideShiftSwapService sideShiftSwapService;

    @Override
    public SwapToLiquidResult createSwap(SwapToLiquidRequest request) {
        SideShiftShiftResponse response = sideShiftSwapService.swapToLiquid(
                request.depositCoin(),
                request.depositNetwork(),
                request.settleAddress(),
                request.refundAddress(),
                request.processId(),
                request.ownerId()
        );
        return new SwapToLiquidResult(
                response.id(),
                response.depositAddress(),
                response.depositCoin(),
                response.depositNetwork(),
                response.settleCoin(),
                response.settleNetwork()
        );
    }
}
