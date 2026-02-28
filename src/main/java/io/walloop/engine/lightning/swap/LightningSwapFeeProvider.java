package io.walloop.engine.lightning.swap;

public interface LightningSwapFeeProvider {

    LightningSwapFeeQuote quoteInvoice(long balanceSats);
}

