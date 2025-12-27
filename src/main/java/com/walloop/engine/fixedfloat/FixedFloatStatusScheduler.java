package com.walloop.engine.fixedfloat;

import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedFloatStatusScheduler {

    private final FixedFloatOrderRepository orderRepository;
    private final FixedFloatOrderService orderService;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final WorkflowOrchestrator orchestrator;
    private final WalloopEngineWorkflow workflow;

    @Scheduled(cron = "${fixedfloat.status-cron:0 * * * * *}")
    public void pollOrders() {
        List<FixedFloatOrderEntity> orders = orderRepository.findByCompletedAtIsNull();
        if (orders.isEmpty()) {
            return;
        }

        for (FixedFloatOrderEntity order : orders) {
            try {
                FixedFloatOrderEntity updated = orderService.refreshOrder(order);
                if (updated.getCompletedAt() != null) {
                    resumeWorkflow(updated.getProcessId());
                }
            } catch (Exception e) {
                log.warn("Failed to poll FixedFloat orderId={}", order.getOrderId(), e);
            }
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
        log.info("Workflow resumed after FixedFloat completion processId={} executionId={}", processId, execution.getId());
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
