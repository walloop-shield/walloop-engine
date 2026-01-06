package com.walloop.engine.boltz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import com.walloop.engine.lightning.LightningInvoiceStatus;
import com.walloop.engine.liquid.service.LiquidRpcService;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BoltzStatusScheduler {

    private final BoltzClient boltzClient;
    private final LightningInvoiceRepository lightningInvoiceRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final WorkflowOrchestrator orchestrator;
    private final ObjectProvider<WalloopEngineWorkflow> workflowProvider;
    private final ObjectMapper objectMapper;
    private final LiquidRpcService liquidRpcService;
    private final TaskScheduler taskScheduler;
    @Value("${boltz.paid-status:invoice.paid}")
    private String paidStatus;
    @Value("${boltz.status-cron:0 * * * * *}")
    private String statusCron;
    @Value("${walloop.engine.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduled;

    public BoltzStatusScheduler(
            BoltzClient boltzClient,
            LightningInvoiceRepository lightningInvoiceRepository,
            WorkflowExecutionRepository workflowExecutionRepository,
            WalletTransactionQueryService walletTransactionQueryService,
            WorkflowOrchestrator orchestrator,
            ObjectProvider<WalloopEngineWorkflow> workflowProvider,
            ObjectMapper objectMapper,
            LiquidRpcService liquidRpcService,
            TaskScheduler taskScheduler
    ) {
        this.boltzClient = boltzClient;
        this.lightningInvoiceRepository = lightningInvoiceRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.walletTransactionQueryService = walletTransactionQueryService;
        this.orchestrator = orchestrator;
        this.workflowProvider = workflowProvider;
        this.objectMapper = objectMapper;
        this.liquidRpcService = liquidRpcService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    void startIfPending() {
        if (!schedulerEnabled) {
            return;
        }
        if (lightningInvoiceRepository.existsByBoltzSwapIdIsNotNullAndStatusNot(LightningInvoiceStatus.PAID)) {
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
            boolean hasPending = pollBoltzStatuses();
            if (!hasPending) {
                stopPolling();
            }
        } finally {
            running.set(false);
        }
    }

    boolean pollBoltzStatuses() {
        List<LightningInvoiceEntity> invoices = lightningInvoiceRepository
                .findByBoltzSwapIdIsNotNullAndStatusNot(LightningInvoiceStatus.PAID);
        if (invoices.isEmpty()) {
            return false;
        }

        boolean pendingLeft = false;
        for (LightningInvoiceEntity invoice : invoices) {
            try {
                BoltzSwapStatusResponse response = boltzClient.getSwapStatus(invoice.getBoltzSwapId());
                if (response != null) {
                    invoice.setBoltzStatus(response.status());
                    invoice.setBoltzStatusPayload(toJson(response));
                    invoice.setUpdatedAt(OffsetDateTime.now());

                    if (isPaid(response)) {
                        enrichPaidTransaction(invoice, response);
                        invoice.setStatus(LightningInvoiceStatus.PAID);
                        invoice.setBoltzPaidAt(OffsetDateTime.now());
                        lightningInvoiceRepository.save(invoice);
                        resumeWorkflow(invoice.getProcessId());
                    } else {
                        lightningInvoiceRepository.save(invoice);
                        pendingLeft = true;
                    }
                } else {
                    pendingLeft = true;
                }
            } catch (Exception e) {
                log.warn("Failed to poll Boltz status for swapId={}", invoice.getBoltzSwapId(), e);
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

    private boolean isPaid(BoltzSwapStatusResponse response) {
        return response.status() != null && response.status().equalsIgnoreCase(paidStatus);
    }

    private void enrichPaidTransaction(LightningInvoiceEntity invoice, BoltzSwapStatusResponse response) {
        BoltzSwapTransaction transaction = response.transaction();
        if (transaction == null || transaction.hex() == null || transaction.hex().isBlank()) {
            return;
        }

        Object decoded = liquidRpcService.decodeRawTransaction(transaction.hex());
        invoice.setBoltzDecodedTransactionPayload(toJson(decoded));
        Long paidAmountSats = extractPaidAmountSats(decoded, invoice.getBoltzLockupAddress());
        if (paidAmountSats != null) {
            invoice.setBoltzPaidAmountSats(paidAmountSats);
        }
    }

    private Long extractPaidAmountSats(Object decoded, String lockupAddress) {
        if (!(decoded instanceof Map<?, ?> decodedMap)) {
            return null;
        }
        Object voutObj = decodedMap.get("vout");
        if (!(voutObj instanceof Iterable<?> vouts)) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        boolean matchedOutput = false;
        for (Object voutItem : vouts) {
            if (!(voutItem instanceof Map<?, ?> vout)) {
                continue;
            }
            BigDecimal value = toBigDecimal(vout.get("value"));
            if (value == null) {
                continue;
            }

            if (lockupAddress != null && !lockupAddress.isBlank()) {
                if (!outputMatchesAddress(vout, lockupAddress)) {
                    continue;
                }
                matchedOutput = true;
            }

            total = total.add(value);
        }

        if (lockupAddress != null && !lockupAddress.isBlank() && !matchedOutput) {
            return null;
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return total.movePointRight(8).setScale(0, RoundingMode.DOWN).longValue();
    }

    private boolean outputMatchesAddress(Map<?, ?> vout, String lockupAddress) {
        Object scriptPubKeyObj = vout.get("scriptPubKey");
        if (!(scriptPubKeyObj instanceof Map<?, ?> scriptPubKey)) {
            return false;
        }
        Object addressesObj = scriptPubKey.get("addresses");
        if (!(addressesObj instanceof Iterable<?> addresses)) {
            return false;
        }
        for (Object address : addresses) {
            if (address != null && lockupAddress.equals(address.toString())) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
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
        WalloopEngineWorkflow workflow = workflowProvider.getObject();
        orchestrator.resume(execution.getId(), workflow, context);
        log.info("Workflow resumed after Boltz payment processId={} executionId={}", processId, execution.getId());
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
