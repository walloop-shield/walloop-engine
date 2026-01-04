package com.walloop.engine.messaging;

import com.walloop.engine.sideshift.SideShiftShiftEntity;
import com.walloop.engine.sideshift.SideShiftShiftRepository;
import com.walloop.engine.sideshift.SideShiftShiftStatus;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.withdrawal.WalloopWithdrawalEntity;
import com.walloop.engine.withdrawal.WalloopWithdrawalRepository;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
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

    private final SideShiftShiftRepository shiftRepository;
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

        SideShiftShiftEntity shift = shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElse(null);
        if (shift != null && shift.getStatus() != SideShiftShiftStatus.SETTLED) {
            shift.setWithdrawCompletedAt(OffsetDateTime.now());
            shift.setStatus(SideShiftShiftStatus.WITHDRAW_COMPLETED);
            shift.setUpdatedAt(OffsetDateTime.now());
            shiftRepository.save(shift);
            handled = true;
            log.info("Withdraw completed for processId={} destination=SIDESHIFT", processId);
        }

        WalloopWithdrawalEntity withdrawal = withdrawalRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElse(null);
        if (withdrawal != null && withdrawal.getCompletedAt() == null) {
            withdrawal.setCompletedAt(OffsetDateTime.now());
            withdrawal.setUpdatedAt(OffsetDateTime.now());
            withdrawalRepository.save(withdrawal);
            handled = true;
            log.info("Withdraw completed for processId={} destination=WALLOOP", processId);
            resumeWorkflow(processId);
        }

        if (!handled) {
            log.warn("Withdraw completed for unknown processId={}", processId);
        }
    }

    private void resumeWorkflow(UUID processId) {
        WorkflowExecution execution = workflowExecutionRepository.findByTransactionId(processId)
                .orElse(null);
        if (execution == null) {
            log.warn("Workflow execution not found for processId={}", processId);
            return;
        }
        UUID ownerId = execution.getOwnerId();
        if (ownerId == null) {
            log.warn("OwnerId missing for processId={}", processId);
            return;
        }

        WorkflowContext context = buildContext(processId, ownerId);
        orchestrator.resume(execution.getId(), workflow, context);
        log.info("Workflow resumed after withdraw completion processId={} executionId={}", processId, execution.getId());
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
