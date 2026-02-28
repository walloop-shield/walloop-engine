package io.walloop.engine.lightning.swap;

public interface LightningSwapStatusPoller {

    boolean hasPending();

    boolean poll();
}

