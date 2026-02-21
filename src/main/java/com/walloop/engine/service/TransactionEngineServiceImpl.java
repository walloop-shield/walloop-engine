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
                message.getProcessId(),
                message.getOwnerId()
        );

        // TODO se transação não existe não pode seguir

        WorkflowContext context = new WorkflowContext();
        context.put(WalloopWorkflowContextKeys.PROCESS_ID, message.getProcessId());
        context.put(WalloopWorkflowContextKeys.OWNER_ID, message.getOwnerId());
        context.put(WalloopWorkflowContextKeys.CHAIN, tx.chain());
        context.put(WalloopWorkflowContextKeys.CORRELATED_ADDRESS, tx.correlatedAddress());
        context.put(WalloopWorkflowContextKeys.DESTINATION_ADDRESS, tx.newAddress2());
        context.put(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, tx.newAddress());

        WorkflowExecution execution = orchestrator.start(
                workflow,
                context,
                new WorkflowStartMetadata(message.getProcessId(), message.getOwnerId())
        );
        log.info(
                "TransactionEngineServiceImpl - Workflow started: executionId={} workflow={} status={} processId={} chain={}",
                execution.getId(),
                execution.getWorkflowName(),
                execution.getStatus(),
                message.getProcessId(),
                tx.chain());
    }
}
