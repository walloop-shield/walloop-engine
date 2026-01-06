package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.fixedfloat.FixedFloatOrderEntity;
import com.walloop.engine.fixedfloat.FixedFloatOrderService;
import com.walloop.engine.fixedfloat.FixedFloatStatusScheduler;
import com.walloop.engine.lightning.LightningInvoiceEntity;
import com.walloop.engine.lightning.LightningInvoiceRepository;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConvertLightningToWalloopStep implements WorkflowStep {

    private final LightningInvoiceRepository lightningInvoiceRepository;
    private final FixedFloatOrderService fixedFloatOrderService;
    private final FixedFloatStatusScheduler fixedFloatStatusScheduler;

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
                .orElseThrow(() -> new IllegalStateException("Lightning invoice not found for processId=" + processId));
        Long paidAmountSats = invoice.getBoltzPaidAmountSats();
        if (paidAmountSats == null || paidAmountSats <= 0) {
            throw new IllegalStateException("Boltz paid amount not available for processId=" + processId);
        }

        FixedFloatOrderEntity order = fixedFloatOrderService.createOrGetOrder(
                processId,
                chain,
                destinationAddress,
                paidAmountSats
        );
        fixedFloatStatusScheduler.ensurePolling();

        if (fixedFloatOrderService.isCompleted(order)) {
            log.info("FixedFloat order completed processId={} orderId={}", processId, order.getOrderId());
            return StepResult.completed("Funds sent to correlated wallet");
        }

        log.info("FixedFloat order pending processId={} orderId={}", processId, order.getOrderId());
        return StepResult.waiting("Awaiting FixedFloat confirmation");
    }
}

