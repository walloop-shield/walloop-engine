package com.walloop.engine.fixedfloat;

import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
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
    private final ObjectProvider<WalloopEngineWorkflow> workflowProvider;
    private final TaskScheduler taskScheduler;

    @Value("${fixedfloat.status-cron:0 * * * * *}")
    private String statusCron;
    @Value("${walloop.engine.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduled;

    @PostConstruct
    void startIfPending() {
        if (!schedulerEnabled) {
            return;
        }
        if (orderRepository.existsByCompletedAtIsNull()) {
            ensurePolling();
        }
    }

    public void ensurePolling() {
        if (scheduled != null && !scheduled.isCancelled()) {
            return;
        }
        scheduled = taskScheduler.schedule(this::pollSafely, new CronTrigger(statusCron));
    }

    void pollSafely() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            boolean hasPending = pollOrders();
            if (!hasPending) {
                stopPolling();
            }
        } finally {
            running.set(false);
        }
    }

    boolean pollOrders() {
        List<FixedFloatOrderEntity> orders = orderRepository.findByCompletedAtIsNull();
        if (orders.isEmpty()) {
            return false;
        }

        boolean pendingLeft = false;
        for (FixedFloatOrderEntity order : orders) {
            try {
                FixedFloatOrderEntity updated = orderService.refreshOrder(order);
                if (updated.getCompletedAt() != null) {
                    resumeWorkflow(updated.getProcessId());
                } else {
                    pendingLeft = true;
                }
            } catch (Exception e) {
                log.warn("FixedFloatStatusScheduler - Failed to poll FixedFloat orderId={}", order.getOrderId(), e);
                pendingLeft = true;
            }
        }
        return pendingLeft;
    }

    private void stopPolling() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }

    private void resumeWorkflow(UUID processId) {
        WorkflowExecution execution = workflowExecutionRepository.findByTransactionId(processId)
                .orElse(null);
        if (execution == null) {
            log.warn("FixedFloatStatusScheduler - Workflow execution not found for processId={}", processId);
            return;
        }
        UUID ownerId = execution.getOwnerId();
        if (ownerId == null) {
            log.warn("FixedFloatStatusScheduler - OwnerId missing for processId={}", processId);
            return;
        }

        WorkflowContext context = buildContext(processId, ownerId);
        WalloopEngineWorkflow workflow = workflowProvider.getObject();
        orchestrator.resume(execution.getId(), workflow, context);
        log.info("FixedFloatStatusScheduler - Workflow resumed after FixedFloat completion processId={} executionId={}", processId, execution.getId());
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
