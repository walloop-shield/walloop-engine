package com.walloop.engine.sideshift;

import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SideShiftStatusScheduler {

    private final SideShiftShiftRepository shiftRepository;
    private final SideShiftClient client;
    private final SideShiftProperties properties;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final WorkflowOrchestrator orchestrator;
    private final WalloopEngineWorkflow workflow;
    private final TaskScheduler taskScheduler;

    @Value("${sideshift.status-cron:0 * * * * *}")
    private String statusCron;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduled;

    @PostConstruct
    void startIfPending() {
        if (shiftRepository.existsByStatusIsNullOrStatusNot(SideShiftShiftStatus.SETTLED)) {
            ensurePolling();
        }
    }

    public void ensurePolling() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            return;
        }
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
            boolean hasPending = pollShiftStatuses();
            if (!hasPending) {
                stopPolling();
            }
        } finally {
            running.set(false);
        }
    }

    boolean pollShiftStatuses() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            log.debug("SideShift secret not configured; skipping status polling");
            return false;
        }

        List<SideShiftShiftEntity> shifts = shiftRepository.findByStatusIsNullOrStatusNot(SideShiftShiftStatus.SETTLED);
        if (shifts.isEmpty()) {
            return false;
        }

        boolean pendingLeft = false;
        for (SideShiftShiftEntity shift : shifts) {
            if (shift.getShiftId() == null || shift.getShiftId().isBlank()) {
                pendingLeft = true;
                continue;
            }
            try {
                SideShiftShiftStatusResponse response = client.getShift(secret, shift.getUserIp(), shift.getShiftId());
                if (isSettled(response)) {
                    markSettledAndResume(shift);
                } else {
                    pendingLeft = true;
                }
            } catch (Exception e) {
                log.warn("Failed to poll SideShift shiftId={} processId={}", shift.getShiftId(), shift.getProcessId(), e);
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

    private boolean isSettled(SideShiftShiftStatusResponse response) {
        if (response == null) {
            return false;
        }
        if ("settled".equalsIgnoreCase(response.status())) {
            return true;
        }
        if (response.deposits() == null) {
            return false;
        }
        return response.deposits().stream()
                .anyMatch(deposit -> "settled".equalsIgnoreCase(deposit.status()));
    }

    private void markSettledAndResume(SideShiftShiftEntity shift) {
        shift.setStatus(SideShiftShiftStatus.SETTLED);
        shift.setSettledAt(OffsetDateTime.now());
        shift.setUpdatedAt(OffsetDateTime.now());
        shiftRepository.save(shift);

        UUID processId = shift.getProcessId();
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
        log.info("Workflow resumed after SideShift settlement processId={} executionId={}", processId, execution.getId());
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
