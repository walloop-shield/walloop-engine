package com.walloop.engine.boltz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import com.walloop.engine.lightning.LightningInvoiceStatus;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoltzStatusScheduler {

    private final BoltzClient boltzClient;
    private final LightningInvoiceRepository lightningInvoiceRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final WorkflowOrchestrator orchestrator;
    private final WalloopEngineWorkflow workflow;
    private final ObjectMapper objectMapper;
    @Value("${boltz.paid-status:invoice.paid}")
    private String paidStatus;

    @Scheduled(cron = "${boltz.status-cron:0 * * * * *}")
    public void pollBoltzStatuses() {
        List<LightningInvoiceEntity> invoices = lightningInvoiceRepository
                .findByBoltzSwapIdIsNotNullAndStatusNot(LightningInvoiceStatus.PAID);
        if (invoices.isEmpty()) {
            return;
        }

        for (LightningInvoiceEntity invoice : invoices) {
            try {
                BoltzSwapStatusResponse response = boltzClient.getSwapStatus(invoice.getBoltzSwapId());
                if (response != null) {
                    invoice.setBoltzStatus(response.status());
                    invoice.setBoltzStatusPayload(toJson(response));
                    invoice.setUpdatedAt(OffsetDateTime.now());

                    if (isPaid(response)) {
                        invoice.setStatus(LightningInvoiceStatus.PAID);
                        invoice.setBoltzPaidAt(OffsetDateTime.now());
                        lightningInvoiceRepository.save(invoice);
                        resumeWorkflow(invoice.getProcessId());
                    } else {
                        lightningInvoiceRepository.save(invoice);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to poll Boltz status for swapId={}", invoice.getBoltzSwapId(), e);
            }
        }
    }

    private boolean isPaid(BoltzSwapStatusResponse response) {
        return response.status() != null && response.status().equalsIgnoreCase(paidStatus);
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
        log.info("Workflow resumed after Boltz payment processId={} executionId={}", processId, execution.getId());
    }

    private WorkflowContext buildContext(UUID processId, UUID ownerId) {
        WalletTransactionDetails tx = walletTransactionQueryService.require(processId, ownerId);
        WorkflowContext context = new WorkflowContext();
        context.put(WalloopWorkflowContextKeys.PROCESS_ID, processId);
        context.put(WalloopWorkflowContextKeys.OWNER_ID, ownerId);
        context.put(WalloopWorkflowContextKeys.CHAIN, tx.chain());
        context.put(WalloopWorkflowContextKeys.CORRELATED_ADDRESS, tx.correlatedAddress());
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
