package com.walloop.engine.swap;

import java.util.List;

public interface SwapStatusPoller {

    SwapPartner partner();

    boolean poll(List<SwapOrderEntity> orders);
}
