package io.walloop.engine.lightning;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OutboundLiquidityInvoiceRequest(
        @NotNull @Positive Long amountSats,
        String note
) {
}

