package io.walloop.engine.service;

import io.walloop.engine.dto.TransactionStartMessage;

public interface TransactionEngineService {

    void handleTransactionStart(TransactionStartMessage message);
}


