package io.walloop.engine.messaging;

import io.walloop.engine.swap.SwapOrderEntity;
import io.walloop.engine.swap.SwapOrderRepository;
import io.walloop.engine.swap.SwapOrderStatus;
import io.walloop.engine.transaction.dto.WalletTransactionDetails;
import io.walloop.engine.transaction.service.WalletTransactionQueryService;
import io.walloop.engine.withdrawal.WalloopWithdrawalEntity;
import io.walloop.engine.withdrawal.WalloopWithdrawalRepository;
import io.walloop.engine.workflow.WorkflowContext;
import io.walloop.engine.workflow.WorkflowExecution;
import io.walloop.engine.workflow.WorkflowExecutionRepository;
import io.walloop.engine.workflow.WorkflowOrchestrator;
import io.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import io.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawCompletedConsumer {

    private final SwapOrderRepository swapOrderRepository;
    private final WalloopWithdrawalRepository withdrawalRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final WorkflowOrchestrator orchestrator;
    private final WalloopEngineWorkflow workflow;

    @RabbitListener(
            queues = WithdrawMessagingConfiguration.ENGINE_WITHDRAW_QUEUE,
            containerFactory = TransactionEngineMessagingConfiguration.TRANSACTION_ENGINE_LISTENER_CONTAINER_FACTORY
    )
    public void onWithdrawCompleted(WithdrawCompletedMessage message) {
        UUID processId = message.processId();
        boolean handled = false;

        SwapOrderEntity shift = swapOrderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElse(null);
        if (shift != null && shift.getStatus() != SwapOrderStatus.SETTLED) {
            shift.setWithdrawCompletedAt(OffsetDateTime.now());
            shift.setStatus(SwapOrderStatus.WITHDRAW_COMPLETED);
            shift.setUpdatedAt(OffsetDateTime.now());
            swapOrderRepository.save(shift);
            handled = true;
            log.info(
                    "WithdrawCompletedConsumer - withdraw completed - processId={} destination={}",
                    processId,
                    shift.getPartner()
            );
        }

        WalloopWithdrawalEntity withdrawal = withdrawalRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElse(null);
        if (withdrawal != null && withdrawal.getCompletedAt() == null) {
            withdrawal.setCompletedAt(OffsetDateTime.now());
            withdrawal.setUpdatedAt(OffsetDateTime.now());
            withdrawalRepository.save(withdrawal);
            handled = true;
            log.info("WithdrawCompletedConsumer - withdraw completed - processId={} destination=TO_PRINCIPAL_WALLET", processId);
            resumeWorkflow(processId);
        }

        if (!handled) {
            log.warn("WithdrawCompletedConsumer - withdraw completed - processId={} destination=UNKNOWN", processId);
        }
    }

    private void resumeWorkflow(UUID processId) {
        WorkflowExecution execution = workflowExecutionRepository.findByTransactionId(processId)
                .orElse(null);
        if (execution == null) {
            log.warn("WithdrawCompletedConsumer - workflow execution missing - processId={}", processId);
            return;
        }
        UUID ownerId = execution.getOwnerId();
        if (ownerId == null) {
            log.warn("WithdrawCompletedConsumer - ownerId missing - processId={}", processId);
            return;
        }

        WorkflowContext context = buildContext(processId, ownerId);
        orchestrator.resume(execution.getId(), workflow, context);
        log.info(
                "WithdrawCompletedConsumer - workflow resumed - processId={} executionId={}",
                processId,
                execution.getId()
        );
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

