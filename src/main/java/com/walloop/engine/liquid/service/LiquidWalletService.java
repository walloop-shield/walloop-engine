package com.walloop.engine.liquid.service;

import com.walloop.engine.liquid.entity.LiquidWalletEntity;
import java.util.UUID;

public interface LiquidWalletService {

    LiquidWalletEntity createForTransaction(UUID transactionId, UUID ownerId);
}

