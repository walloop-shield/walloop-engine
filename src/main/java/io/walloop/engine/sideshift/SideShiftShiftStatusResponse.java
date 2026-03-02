package io.walloop.engine.sideshift;

import java.util.List;

public record SideShiftShiftStatusResponse(
        String id,
        String status,
        List<SideShiftDepositStatus> deposits
) {
}

