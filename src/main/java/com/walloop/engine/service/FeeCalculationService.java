package com.walloop.engine.service;

import java.util.UUID;

public interface FeeCalculationService {

    long calculateFeeSats(UUID transactionId, UUID ownerId);
}

