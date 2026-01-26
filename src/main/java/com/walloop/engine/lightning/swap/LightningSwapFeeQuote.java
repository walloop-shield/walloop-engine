package com.walloop.engine.lightning.swap;

public record LightningSwapFeeQuote(
        long invoiceSats,
        double percentage,
        long minerFees,
        String pairHash
) {
}
