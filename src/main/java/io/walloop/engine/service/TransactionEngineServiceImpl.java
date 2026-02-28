package io.walloop.engine.service;

import io.walloop.engine.dto.TransactionStartMessage;
import io.walloop.engine.transaction.dto.WalletTransactionDetails;
import io.walloop.engine.transaction.service.WalletTransactionQueryService;
import io.walloop.engine.workflow.WorkflowContext;
import io.walloop.engine.workflow.WorkflowExecution;
import io.walloop.engine.workflow.WorkflowOrchestrator;
import io.walloop.engine.workflow.WorkflowStartMetadata;
import io.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import io.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
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
        WalletTransactionDetails tx = walletTransactionQueryService
                .find(message.getProcessId(), message.getOwnerId())
                .orElseThrow(() -> new IllegalStateException(
                        "TransactionEngineServiceImpl - transaction not found - processId="
                                + message.getProcessId()
                                + " ownerId="
                                + message.getOwnerId()
                ));

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


