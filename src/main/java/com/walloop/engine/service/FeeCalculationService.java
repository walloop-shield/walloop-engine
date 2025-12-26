package com.walloop.engine.service;

import java.util.UUID;

public interface FeeCalculationService {

    long calculateFee(UUID transactionId, UUID ownerId);
}
