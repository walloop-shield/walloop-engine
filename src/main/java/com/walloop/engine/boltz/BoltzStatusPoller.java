package com.walloop.engine.boltz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import com.walloop.engine.lightning.LightningInvoiceStatus;
import com.walloop.engine.lightning.swap.LightningSwapStatusPoller;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.Invoice;
import org.lightningj.lnd.wrapper.message.PayReq;
import org.lightningj.lnd.wrapper.message.PaymentHash;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoltzStatusPoller implements LightningSwapStatusPoller {

    private static final String PARTNER = "BOLTZ";

    private final BoltzClient boltzClient;
    private final LightningInvoiceRepository lightningInvoiceRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final WorkflowOrchestrator orchestrator;
    private final ObjectProvider<WalloopEngineWorkflow> workflowProvider;
    private final ObjectMapper objectMapper;
    private final SynchronousLndAPI lndApi;

    @Value("${boltz.paid-status:invoice.paid}")
    private String paidStatus;

    @Override
    public boolean hasPending() {
        return lightningInvoiceRepository.existsBySwapPartnerAndSwapIdIsNotNullAndStatusNot(
                PARTNER,
                LightningInvoiceStatus.PAID
        );
    }

    @Override
    public boolean poll() {
        List<LightningInvoiceEntity> invoices = lightningInvoiceRepository
                .findBySwapPartnerAndSwapIdIsNotNullAndStatusNot(PARTNER, LightningInvoiceStatus.PAID);
        if (invoices.isEmpty()) {
            return false;
        }

        boolean pendingLeft = false;
        for (LightningInvoiceEntity invoice : invoices) {
            try {
                BoltzSwapStatusResponse response = boltzClient.getSwapStatus(invoice.getSwapId());
                if (response != null) {
                    invoice.setSwapStatus(response.status());
                    invoice.setSwapStatusPayload(toJson(response));
                    invoice.setUpdatedAt(OffsetDateTime.now());

                    if (isPaid(response)) {
                        boolean paidAmountUpdated = enrichPaidTransaction(invoice);
                        if (paidAmountUpdated) {
                            invoice.setStatus(LightningInvoiceStatus.PAID);
                            invoice.setSwapPaidAt(OffsetDateTime.now());
                            lightningInvoiceRepository.save(invoice);
                            resumeWorkflow(invoice.getProcessId());
                        } else {
                            lightningInvoiceRepository.save(invoice);
                            pendingLeft = true;
                        }
                    } else {
                        lightningInvoiceRepository.save(invoice);
                        pendingLeft = true;
                    }
                } else {
                    pendingLeft = true;
                }
            } catch (Exception e) {
                log.warn(
                        "BoltzStatusPoller - status poll failed - swapId={} processId={}",
                        invoice.getSwapId(),
                        invoice.getProcessId(),
                        e
                );
                pendingLeft = true;
            }
        }
        return pendingLeft;
    }

    private boolean isPaid(BoltzSwapStatusResponse response) {
        return response.status() != null && response.status().equalsIgnoreCase(paidStatus);
    }

    private boolean enrichPaidTransaction(LightningInvoiceEntity invoice) {
        String paymentRequest = invoice.getInvoice();
        if (paymentRequest == null || paymentRequest.isBlank()) {
            return false;
        }
        try {
            PayReq payReq = lndApi.decodePayReq(paymentRequest);
            if (payReq == null || payReq.getPaymentHash() == null || payReq.getPaymentHash().isBlank()) {
                return false;
            }
            PaymentHash paymentHash = new PaymentHash();
            paymentHash.setRHashStr(payReq.getPaymentHash());
            Invoice lookup = lndApi.lookupInvoice(paymentHash);
            if (lookup == null) {
                return false;
            }
            Long paidAmountSats = lookup.getAmtPaidSat();
            if (paidAmountSats == null || paidAmountSats <= 0) {
                return false;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("decodePayReq", buildPayReqPayload(payReq));
            payload.put("lookupInvoice", buildLookupPayload(lookup));
            invoice.setSwapDecodedTransactionPayload(toJson(payload));
            invoice.setSwapPaidAmountSats(paidAmountSats);
            return true;
        } catch (StatusException | ValidationException e) {
            log.warn(
                    "BoltzStatusPoller - invoice lookup failed - swapId={}",
                    invoice.getSwapId(),
                    e
            );
            return false;
        }
    }

    private Map<String, Object> buildPayReqPayload(PayReq payReq) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("destination", payReq.getDestination());
        payload.put("paymentHash", payReq.getPaymentHash());
        payload.put("numSatoshis", payReq.getNumSatoshis());
        payload.put("numMsat", payReq.getNumMsat());
        payload.put("timestamp", payReq.getTimestamp());
        payload.put("expiry", payReq.getExpiry());
        payload.put("description", payReq.getDescription());
        payload.put("descriptionHash", payReq.getDescriptionHash());
        payload.put("fallbackAddr", payReq.getFallbackAddr());
        payload.put("cltvExpiry", payReq.getCltvExpiry());
        payload.put("paymentAddr", bytesToHex(payReq.getPaymentAddr()));
        return payload;
    }

    private Map<String, Object> buildLookupPayload(Invoice lookup) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("memo", lookup.getMemo());
        payload.put("rHash", bytesToHex(lookup.getRHash()));
        payload.put("rPreimage", bytesToHex(lookup.getRPreimage()));
        payload.put("value", lookup.getValue());
        payload.put("valueMsat", lookup.getValueMsat());
        payload.put("settled", lookup.getSettled());
        payload.put("creationDate", lookup.getCreationDate());
        payload.put("settleDate", lookup.getSettleDate());
        payload.put("paymentRequest", lookup.getPaymentRequest());
        payload.put("expiry", lookup.getExpiry());
        payload.put("fallbackAddr", lookup.getFallbackAddr());
        payload.put("cltvExpiry", lookup.getCltvExpiry());
        payload.put("private", lookup.getPrivate());
        payload.put("addIndex", lookup.getAddIndex());
        payload.put("settleIndex", lookup.getSettleIndex());
        payload.put("amtPaid", lookup.getAmtPaid());
        payload.put("amtPaidSat", lookup.getAmtPaidSat());
        payload.put("amtPaidMsat", lookup.getAmtPaidMsat());
        payload.put("state", lookup.getState() != null ? lookup.getState().name() : null);
        payload.put("paymentAddr", bytesToHex(lookup.getPaymentAddr()));
        payload.put("isKeysend", lookup.getIsKeysend());
        payload.put("isAmp", lookup.getIsAmp());
        return payload;
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private void resumeWorkflow(UUID processId) {
        WorkflowExecution execution = workflowExecutionRepository.findByTransactionId(processId)
                .orElse(null);
        if (execution == null) {
            log.warn("BoltzStatusPoller - workflow execution missing - processId={}", processId);
            return;
        }
        UUID ownerId = execution.getOwnerId();
        if (ownerId == null) {
            log.warn("BoltzStatusPoller - ownerId missing - processId={}", processId);
            return;
        }

        WorkflowContext context = buildContext(processId, ownerId);
        WalloopEngineWorkflow workflow = workflowProvider.getObject();
        orchestrator.resume(execution.getId(), workflow, context);
        log.info(
                "BoltzStatusPoller - workflow resumed - processId={} executionId={}",
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
