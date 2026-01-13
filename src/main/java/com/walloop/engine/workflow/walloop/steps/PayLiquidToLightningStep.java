package com.walloop.engine.workflow.walloop.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.boltz.BoltzClient;
import com.walloop.engine.boltz.BoltzSubmarineRequest;
import com.walloop.engine.boltz.BoltzSubmarineResponse;
import com.walloop.engine.boltz.BoltzStatusScheduler;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import com.walloop.engine.lightning.LightningInvoiceStatus;
import com.walloop.engine.liquid.entity.LiquidWalletEntity;
import com.walloop.engine.liquid.repository.LiquidWalletRepository;
import com.walloop.engine.liquid.service.LiquidRpcService;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayLiquidToLightningStep implements WorkflowStep {

    private static final String BOLTZ_FROM_ASSET = "L-BTC";
    private static final String BOLTZ_TO_ASSET = "BTC";

    private final BoltzClient boltzClient;
    private final LightningInvoiceRepository lightningInvoiceRepository;
    private final LiquidWalletRepository liquidWalletRepository;
    private final LiquidRpcService liquidRpcService;
    private final BoltzStatusScheduler boltzStatusScheduler;
    private final ObjectMapper objectMapper;

    @Override
    public String key() {
        return "pay_liquid_to_lightning";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);

        LightningInvoiceEntity invoiceEntity = lightningInvoiceRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElseThrow(() -> new IllegalStateException("Lightning invoice not found for processId=" + processId));

        String invoice = context.get(WalloopWorkflowContextKeys.LIGHTNING_INVOICE, String.class)
                .orElse(invoiceEntity.getInvoice());

        if (invoice == null || invoice.isBlank()) {
            throw new IllegalStateException("Lightning invoice not available for processId=" + processId);
        }

        if (LightningInvoiceStatus.PAID.equals(invoiceEntity.getStatus())) {
            return StepResult.completed("Payment to LN confirmed by Boltz");
        }

        if (invoiceEntity.getBoltzSwapId() == null) {
            BoltzSubmarineRequest request = BoltzSubmarineRequest.builder()
                    .from(BOLTZ_FROM_ASSET)
                    .to(BOLTZ_TO_ASSET)
                    .invoice(invoice)
                    .build();
            BoltzSubmarineResponse response = boltzClient.createSubmarineSwap(request);
            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new IllegalStateException("Boltz swap response missing id for processId=" + processId);
            }
            invoiceEntity.setBoltzSwapId(response.id());
            invoiceEntity.setBoltzLockupAddress(response.address());
            invoiceEntity.setBoltzExpectedAmount(response.expectedAmount());
            invoiceEntity.setBoltzRequestPayload(toJson(request));
            invoiceEntity.setBoltzResponsePayload(toJson(response));
            invoiceEntity.setUpdatedAt(OffsetDateTime.now());
            lightningInvoiceRepository.save(invoiceEntity);
            boltzStatusScheduler.ensurePolling();
        }

        if (invoiceEntity.getLiquidTxId() == null) {
            LiquidWalletEntity wallet = liquidWalletRepository.findFirstByTransactionIdOrderByCreatedAtDesc(processId)
                    .orElseThrow(() -> new IllegalStateException("Liquid wallet not found for processId=" + processId));

            String destinationAddress = invoiceEntity.getBoltzLockupAddress();
            Long expectedAmount = invoiceEntity.getBoltzExpectedAmount();
            if (destinationAddress == null || destinationAddress.isBlank() || expectedAmount == null) {
                throw new IllegalStateException("Boltz lockup data missing for processId=" + processId);
            }

            if (wallet.getPrivateKey() != null && !wallet.getPrivateKey().isBlank()) {
                liquidRpcService.importPrivateKey(wallet.getPrivateKey(), "walloop", false);
            }

            String amount = BigDecimal.valueOf(expectedAmount)
                    .movePointLeft(8)
                    .stripTrailingZeros()
                    .toPlainString();
            String txId = liquidRpcService.sendToAddress(destinationAddress, amount);
            invoiceEntity.setLiquidTxId(txId);
            invoiceEntity.setStatus(LightningInvoiceStatus.LOCKUP_SENT);
            invoiceEntity.setUpdatedAt(OffsetDateTime.now());
            lightningInvoiceRepository.save(invoiceEntity);
            log.info("Boltz lockup sent processId={} address={} txId={}", processId, destinationAddress, txId);
        }

        return StepResult.waiting("Waiting for Boltz payment confirmation");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Boltz payload", e);
        }
    }
}
