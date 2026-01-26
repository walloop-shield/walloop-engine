package com.walloop.engine.conversion;

import java.util.List;

public interface ConversionStatusPoller {

    ConversionPartner partner();

    boolean poll(List<ConversionOrderEntity> orders);
}
