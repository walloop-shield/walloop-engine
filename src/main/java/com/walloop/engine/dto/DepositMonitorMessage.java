package com.walloop.engine.dto;

import java.util.UUID;

public record DepositMonitorMessage(
        String address,
        String network,
        UUID owner,
        UUID processId
) {
}
