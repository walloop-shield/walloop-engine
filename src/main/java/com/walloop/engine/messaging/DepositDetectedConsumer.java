package com.walloop.engine.messaging;

import com.walloop.engine.deposit.DepositMonitorEntity;
import com.walloop.engine.deposit.DepositMonitorRepository;
import com.walloop.engine.deposit.DepositMonitorStatus;
import com.walloop.engine.dto.DepositDetectedMessage;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositDetectedConsumer {

    private final DepositMonitorRepository depositMonitorRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final WorkflowOrchestrator orchestrator;
    private final WalloopEngineWorkflow workflow;

    @RabbitListener(
            queues = DepositMonitoringMessagingConfiguration.DEPOSIT_DETECTED_QUEUE,
            containerFactory = TransactionEngineMessagingConfiguration.TX_ENGINE_LISTENER_CONTAINER_FACTORY
    )
    public void onDepositDetected(DepositDetectedMessage message) {
        UUID processId = message.processId();
        Optional<WorkflowExecution> execution = workflowExecutionRepository.findByTransactionId(processId);

        if (execution.isEmpty()) {
            log.warn("Deposit detected for unknown processId={}", processId);
            return;
        }

        WorkflowExecution workflowExecution = execution.get();
        UUID ownerId = workflowExecution.getOwnerId();

        DepositMonitorEntity monitor = depositMonitorRepository.findById(processId)
                .orElseGet(() -> createMonitorFromTransaction(processId, ownerId));

        if (monitor != null) {
            monitor.setStatus(DepositMonitorStatus.DETECTED);
            monitor.setUpdatedAt(OffsetDateTime.now());
            depositMonitorRepository.save(monitor);
        }

        WorkflowContext context = buildContext(processId, ownerId);
        orchestrator.resume(workflowExecution.getId(), workflow, context);
        log.info("Workflow resumed for processId={} executionId={}", processId, workflowExecution.getId());
    }

    private DepositMonitorEntity createMonitorFromTransaction(UUID processId, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        WalletTransactionDetails tx = walletTransactionQueryService.require(processId, ownerId);
        DepositMonitorEntity monitor = new DepositMonitorEntity();
        monitor.setProcessId(processId);
        monitor.setOwnerId(ownerId);
        monitor.setAddress(tx.newAddress());
        monitor.setNetwork(tx.chain());
        monitor.setStatus(DepositMonitorStatus.DETECTED);
        monitor.setCreatedAt(OffsetDateTime.now());
        monitor.setUpdatedAt(OffsetDateTime.now());
        return depositMonitorRepository.save(monitor);
    }

    private WorkflowContext buildContext(UUID processId, UUID ownerId) {
        WalletTransactionDetails tx = walletTransactionQueryService.require(processId, ownerId);
        WorkflowContext context = new WorkflowContext();
        context.put(WalloopWorkflowContextKeys.PROCESS_ID, processId);
        context.put(WalloopWorkflowContextKeys.OWNER_ID, ownerId);
        context.put(WalloopWorkflowContextKeys.CHAIN, tx.chain());
        context.put(WalloopWorkflowContextKeys.CORRELATED_ADDRESS, tx.correlatedAddress());
        context.put(WalloopWorkflowContextKeys.DESTINATION_ADDRESS, tx.newAddress2());
        context.put(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, tx.newAddress());
        return context;
    }
}
