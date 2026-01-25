package com.walloop.engine.messaging;

import com.walloop.engine.dto.TransactionStartMessage;
import com.walloop.engine.service.TransactionEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEngineConsumer {

    private final TransactionEngineService transactionEngineService;

    @RabbitListener(
            queues = TransactionEngineMessagingConfiguration.TRANSACTION_ENGINE_QUEUE,
            containerFactory = TransactionEngineMessagingConfiguration.TRANSACTION_ENGINE_LISTENER_CONTAINER_FACTORY
    )
    public void onTransactionStart(TransactionStartMessage message) {
        log.info(
                "TransactionEngineConsumer - Received transaction start: processId={} ownerId={}",
                message.getProcessId(),
                message.getOwnerId());
        transactionEngineService.handleTransactionStart(message);
    }
}
