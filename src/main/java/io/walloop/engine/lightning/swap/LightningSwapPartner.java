package io.walloop.engine.lightning.swap;

public interface LightningSwapPartner {

    LightningSwapResult createSwap(LightningSwapRequest request);
}

