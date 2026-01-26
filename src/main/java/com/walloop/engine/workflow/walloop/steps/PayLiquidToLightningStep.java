package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.lightning.swap.LightningSwapPartner;
import com.walloop.engine.lightning.swap.LightningSwapRequest;
import com.walloop.engine.lightning.swap.LightningSwapResult;
import com.walloop.engine.lightning.swap.LightningSwapStatusScheduler;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import com.walloop.engine.lightning.LightningInvoiceStatus;
import com.walloop.engine.liquid.entity.LiquidWalletEntity;
import com.walloop.engine.liquid.repository.LiquidWalletRepository;
import com.walloop.engine.liquid.service.LiquidRpcService;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.StepStatus;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.math.BigDecimal;
import java.time.Duration;
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

    private final LightningSwapPartner lightningSwapPartner;
    private final LightningInvoiceRepository lightningInvoiceRepository;
    private final LiquidWalletRepository liquidWalletRepository;
    private final LiquidRpcService liquidRpcService;
    private final LightningSwapStatusScheduler lightningSwapStatusScheduler;
    private final WorkflowExecutionRepository executionRepository;

    private static final int MAX_RETRIES = 5;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);

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
            return StepResult.completed("Lightning payment confirmed by swap partner");
        }

        LiquidWalletEntity wallet = null;
        if (invoiceEntity.getSwapId() == null || invoiceEntity.getLiquidTxId() == null) {
            wallet = liquidWalletRepository.findFirstByTransactionIdOrderByCreatedAtDesc(processId)
                    .orElseThrow(() -> new IllegalStateException("Liquid wallet not found for processId=" + processId));
        }

        if (invoiceEntity.getSwapId() == null) {
            try {
                String refundPubKey = resolveRefundPublicKey(wallet);
                LightningSwapRequest request = new LightningSwapRequest(
                        BOLTZ_FROM_ASSET,
                        BOLTZ_TO_ASSET,
                        invoice,
                        refundPubKey
                );
                LightningSwapResult response = lightningSwapPartner.createSwap(request);
                if (response == null || response.swapId() == null || response.swapId().isBlank()) {
                    return retryOrFail(processId, "Swap partner response missing id", null);
                }
                invoiceEntity.setSwapPartner("BOLTZ");
                invoiceEntity.setSwapId(response.swapId());
                invoiceEntity.setSwapLockupAddress(response.lockupAddress());
                invoiceEntity.setSwapExpectedAmount(response.expectedAmount());
                invoiceEntity.setSwapRequestPayload(response.requestPayload());
                invoiceEntity.setSwapResponsePayload(response.responsePayload());
                invoiceEntity.setUpdatedAt(OffsetDateTime.now());
                lightningInvoiceRepository.save(invoiceEntity);
                lightningSwapStatusScheduler.ensurePolling();
            } catch (RuntimeException e) {
                return retryOrFail(processId, "Swap partner creation failed", e);
            }
        }

        if (invoiceEntity.getLiquidTxId() == null) {
            String destinationAddress = invoiceEntity.getSwapLockupAddress();
            Long expectedAmount = invoiceEntity.getSwapExpectedAmount();
            if (destinationAddress == null || destinationAddress.isBlank() || expectedAmount == null) {
                throw new IllegalStateException("Swap lockup data missing for processId=" + processId);
            }

            try {
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
                log.info(
                        "PayLiquidToLightningStep - lockup sent - processId={} address={} txId={}",
                        processId,
                        destinationAddress,
                        txId
                );
            } catch (RuntimeException e) {
                return retryOrFail(processId, "Liquid lockup transaction failed", e);
            }
        }

        return StepResult.waiting("Waiting for swap partner payment confirmation");
    }

    private String resolveRefundPublicKey(LiquidWalletEntity wallet) {
        if (wallet.getPrivateKey() != null && !wallet.getPrivateKey().isBlank()) {
            liquidRpcService.importPrivateKey(wallet.getPrivateKey(), "walloop", false);
        }
        return liquidRpcService.getAddressPubKey(wallet.getAddress());
    }

    private StepResult retryOrFail(UUID processId, String detail, RuntimeException error) {
        int retries = countRetries(processId);
        if (retries >= MAX_RETRIES) {
            log.warn(
                    "PayLiquidToLightningStep - retry limit reached - processId={} detail={} retries={}",
                    processId,
                    detail,
                    retries,
                    error
            );
            return StepResult.failed(detail + " after retries");
        }
        log.warn(
                "PayLiquidToLightningStep - retry scheduled - processId={} detail={} retry={} maxRetries={}",
                processId,
                detail,
                retries + 1,
                MAX_RETRIES,
                error
        );
        return StepResult.retry(detail, RETRY_DELAY);
    }

    private int countRetries(UUID processId) {
        return executionRepository.findByTransactionId(processId)
                .map(execution -> execution.getHistory().stream()
                        .filter(item -> key().equals(item.stepKey()))
                        .filter(item -> item.status() == StepStatus.RETRY)
                        .count())
                .map(Long::intValue)
                .orElse(0);
    }

}
