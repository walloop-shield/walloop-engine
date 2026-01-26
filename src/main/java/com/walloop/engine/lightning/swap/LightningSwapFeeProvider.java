package com.walloop.engine.lightning.swap;

public interface LightningSwapFeeProvider {

    LightningSwapFeeQuote quoteInvoice(long balanceSats);
}
