package com.walloop.engine.lightning.swap;

public interface LightningSwapStatusPoller {

    boolean hasPending();

    boolean poll();
}
