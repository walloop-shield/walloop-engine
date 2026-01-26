package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.conversion.ConversionOrderEntity;
import com.walloop.engine.conversion.ConversionOrderRepository;
import com.walloop.engine.conversion.ConversionPartnerService;
import com.walloop.engine.conversion.ConversionStatusScheduler;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.SendRequest;
import org.lightningj.lnd.wrapper.message.SendResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConvertLightningToWalloopStep implements WorkflowStep {

    private final LightningInvoiceRepository lightningInvoiceRepository;
    private final ConversionPartnerService conversionPartnerService;
    private final ConversionOrderRepository conversionOrderRepository;
    private final ConversionStatusScheduler conversionStatusScheduler;
    private final SynchronousLndAPI lndApi;

    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_FAILED = "FAILED";
    private static final int MAX_PAYMENT_RETRIES = 5;
    private static final Duration PAYMENT_RETRY_DELAY = Duration.ofMinutes(5);

    @Override
    public String key() {
        return "convert_lightning_to_walloop";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);
        String chain = context.require(WalloopWorkflowContextKeys.CHAIN, String.class);
        String destinationAddress = context.require(WalloopWorkflowContextKeys.DESTINATION_ADDRESS, String.class);

        LightningInvoiceEntity invoice = lightningInvoiceRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElseThrow(() -> new IllegalStateException("Lightning invoice not found"));
        Long paidAmountSats = invoice.getSwapPaidAmountSats();
        if (paidAmountSats == null || paidAmountSats <= 0) {
            throw new IllegalStateException("Swap partner paid amount not available");
        }

        ConversionOrderEntity order = conversionPartnerService.createOrGetOrder(
                processId,
                chain,
                destinationAddress,
                paidAmountSats
        );

        if (!PAYMENT_STATUS_PAID.equals(order.getPaymentStatus())) {
            int attempts = order.getPaymentAttempts() == null ? 0 : order.getPaymentAttempts();
            if (attempts >= MAX_PAYMENT_RETRIES) {
                return StepResult.failed("Conversion partner payment retries exhausted");
            }
            String paymentRequest = order.getPaymentRequest();
            if (paymentRequest == null || paymentRequest.isBlank()) {
                throw new IllegalStateException("Conversion partner payment request not available");
            }
            try {
                order.setPaymentAttempts(attempts + 1);
                order.setPaymentAttemptedAt(OffsetDateTime.now());
                SendRequest request = new SendRequest();
                request.setPaymentRequest(paymentRequest);
                SendResponse response = lndApi.sendPaymentSync(request);
                String paymentError = response.getPaymentError();
                if (paymentError != null && !paymentError.isBlank()) {
                    order.setPaymentStatus(PAYMENT_STATUS_FAILED);
                    order.setPaymentError(paymentError);
                    order.setUpdatedAt(OffsetDateTime.now());
                    conversionOrderRepository.save(order);
                    return StepResult.retry("Conversion partner payment failed [" + paymentError + "]", PAYMENT_RETRY_DELAY);
                }
                order.setPaymentStatus(PAYMENT_STATUS_PAID);
                order.setPaymentPreimage(toHex(response.getPaymentPreimage()));
                order.setPaymentHash(toHex(response.getPaymentHash()));
                order.setPaymentCompletedAt(OffsetDateTime.now());
                order.setUpdatedAt(OffsetDateTime.now());
                conversionOrderRepository.save(order);
            } catch (StatusException | ValidationException e) {
                order.setPaymentStatus(PAYMENT_STATUS_FAILED);
                order.setPaymentError(e.getMessage());
                order.setUpdatedAt(OffsetDateTime.now());
                conversionOrderRepository.save(order);
                return StepResult.retry("Conversion partner payment failed", PAYMENT_RETRY_DELAY);
            }
        }

        conversionStatusScheduler.ensurePolling();

        if (conversionPartnerService.isCompleted(order)) {
            log.info(
                    "ConvertLightningToWalloopStep - Conversion order completed processId={} orderId={} partner={}",
                    processId,
                    order.getPartnerOrderId(),
                    order.getPartner()
            );
            return StepResult.completed("Funds sent to destination wallet");
        }

        log.info(
                "ConvertLightningToWalloopStep - Conversion order pending processId={} orderId={} partner={}",
                processId,
                order.getPartnerOrderId(),
                order.getPartner()
        );
        return StepResult.waiting("Waiting for conversion confirmation");
    }

    private String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}

