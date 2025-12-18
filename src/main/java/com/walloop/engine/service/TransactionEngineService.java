package com.walloop.engine.service;

import com.walloop.engine.dto.TransactionStartMessage;

public interface TransactionEngineService {

    void handleTransactionStart(TransactionStartMessage message);
}

