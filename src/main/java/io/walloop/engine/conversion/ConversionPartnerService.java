package io.walloop.engine.conversion;

import java.util.UUID;

public interface ConversionPartnerService {

    ConversionOrderEntity createOrGetOrder(UUID processId, String chain, String toAddress, long amountSats);

    ConversionOrderEntity refreshOrder(ConversionOrderEntity entity);

    boolean isCompleted(ConversionOrderEntity entity);
}

