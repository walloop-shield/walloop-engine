package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.lightning.LightningInboundLiquidityService;
import com.walloop.engine.lightning.LightningInvoiceService;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateLightningInvoiceStep implements WorkflowStep {

    private final LightningInvoiceService lightningInvoiceService;
    private final LightningInboundLiquidityService inboundLiquidityService;

    @Override
    public String key() {
        return "create_lightning_invoice";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);
        UUID ownerId = context.require(WalloopWorkflowContextKeys.OWNER_ID, UUID.class);

        long requiredSats = lightningInvoiceService.resolveInvoiceAmountSats(processId, ownerId);
        LightningInboundLiquidityService.InboundLiquidityCheck inboundCheck =
                inboundLiquidityService.ensureInboundLiquidity(processId, requiredSats);
        if (!inboundCheck.ready()) {
            Duration retryAfter = inboundCheck.retryAfter() != null ? inboundCheck.retryAfter() : Duration.ofMinutes(5);
            String detail = inboundCheck.detail() != null ? inboundCheck.detail() : "Waiting for inbound liquidity";
            return StepResult.retry(detail, retryAfter);
        }

        String invoice = lightningInvoiceService.createOrGetInvoice(processId, ownerId);
        context.put(WalloopWorkflowContextKeys.LIGHTNING_INVOICE, invoice);
        log.info("Lightning invoice created for processId={} ownerId={}", processId, ownerId);
        return StepResult.completed("Lightning invoice created");
    }
}
