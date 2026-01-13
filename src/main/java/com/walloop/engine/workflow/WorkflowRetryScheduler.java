package com.walloop.engine.workflow;

import com.walloop.engine.liquid.repository.LiquidWalletRepository;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowRetryScheduler {

    private final WorkflowExecutionRepository executionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final LiquidWalletRepository liquidWalletRepository;
    private final WorkflowOrchestrator orchestrator;
    private final ObjectProvider<WalloopEngineWorkflow> workflowProvider;
    private final TaskScheduler taskScheduler;

    @Value("${walloop.engine.retry.cron:*/15 * * * * *}")
    private String retryCron;

    @Value("${walloop.engine.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduled;

    @PostConstruct
    void startIfPending() {
        if (!schedulerEnabled) {
            return;
        }
        if (executionRepository.existsPendingRetries()) {
            ensurePolling();
        }
    }

    @EventListener
    public void onRetryScheduled(WorkflowRetryScheduledEvent event) {
        if (!schedulerEnabled) {
            return;
        }
        ensurePolling();
    }

    public void ensurePolling() {
        if (scheduled != null && !scheduled.isCancelled()) {
            return;
        }
        scheduled = taskScheduler.schedule(this::pollSafely, new CronTrigger(retryCron));
    }

    void pollSafely() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            boolean hasPending = pollRetries();
            if (!hasPending) {
                stopPolling();
            }
        } finally {
            running.set(false);
        }
    }

    boolean pollRetries() {
        List<WorkflowExecution> due = executionRepository.findRetriesDue(Instant.now());
        if (due.isEmpty()) {
            return executionRepository.existsPendingRetries();
        }

        for (WorkflowExecution execution : due) {
            resumeWorkflow(execution);
        }
        return true;
    }

    private void resumeWorkflow(WorkflowExecution execution) {
        UUID processId = execution.getTransactionId();
        UUID ownerId = execution.getOwnerId();
        if (processId == null || ownerId == null) {
            log.warn("Retry skipped, missing identifiers. executionId={}", execution.getId());
            return;
        }

        WalletTransactionDetails tx = walletTransactionQueryService.require(processId, ownerId);
        WorkflowContext context = new WorkflowContext();
        context.put(WalloopWorkflowContextKeys.PROCESS_ID, processId);
        context.put(WalloopWorkflowContextKeys.OWNER_ID, ownerId);
        context.put(WalloopWorkflowContextKeys.CHAIN, tx.chain());
        context.put(WalloopWorkflowContextKeys.CORRELATED_ADDRESS, tx.correlatedAddress());
        context.put(WalloopWorkflowContextKeys.DESTINATION_ADDRESS, tx.newAddress2());
        context.put(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, tx.newAddress());
        liquidWalletRepository.findFirstByTransactionIdOrderByCreatedAtDesc(processId)
                .ifPresent(wallet -> context.put(WalloopWorkflowContextKeys.LIQUID_ADDRESS, wallet.getAddress()));

        WalloopEngineWorkflow workflow = workflowProvider.getObject();
        orchestrator.resume(execution.getId(), workflow, context);
        log.info("Workflow retried executionId={} processId={}", execution.getId(), processId);
    }

    private void stopPolling() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }
}
