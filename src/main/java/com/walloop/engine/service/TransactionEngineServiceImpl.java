package com.walloop.engine.service;

import com.walloop.engine.dto.TransactionStartMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEngineServiceImpl implements TransactionEngineService {

    @Override
    public void handleTransactionStart(TransactionStartMessage message) {
        log.info(
                "Handling transaction start message: transactionId={} ownerId={}",
                message.getTransactionId(),
                message.getOwnerId());
        // próximos passos: buscar dados, orquestrar fluxo, persistir estado/projeções, etc.
    }
}

