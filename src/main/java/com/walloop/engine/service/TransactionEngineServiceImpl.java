package com.walloop.engine.service;

import com.walloop.engine.dto.TransactionStartMessage;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.WorkflowStartMetadata;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEngineServiceImpl implements TransactionEngineService {

    private final WorkflowOrchestrator orchestrator;
    private final WalloopEngineWorkflow workflow;
    private final WalletTransactionQueryService walletTransactionQueryService;

    @Override
    public void handleTransactionStart(TransactionStartMessage message) {
        WalletTransactionDetails tx = walletTransactionQueryService.require(
                message.getTransactionId(),
                message.getOwnerId()
        );

        WorkflowContext context = new WorkflowContext();
        context.put(WalloopWorkflowContextKeys.TRANSACTION_ID, message.getTransactionId());
        context.put(WalloopWorkflowContextKeys.OWNER_ID, message.getOwnerId());
        context.put(WalloopWorkflowContextKeys.CHAIN, tx.chain());
        context.put(WalloopWorkflowContextKeys.CORRELATED_ADDRESS, tx.correlatedAddress());
        context.put(WalloopWorkflowContextKeys.NEW_ADDRESS, tx.newAddress());
        context.put(WalloopWorkflowContextKeys.WALLOOP_DEPOSIT_DETECTED, false);

        WorkflowExecution execution = orchestrator.start(
                workflow,
                context,
                new WorkflowStartMetadata(message.getTransactionId(), message.getOwnerId())
        );
        log.info(
                "Workflow started: executionId={} workflow={} status={} transactionId={}",
                execution.getId(),
                execution.getWorkflowName(),
                execution.getStatus(),
                message.getTransactionId());
    }
}
