package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.service.FeeCalculationService;
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
public class CalculateFeesStep implements WorkflowStep {

    private final FeeCalculationService feeCalculationService;

    @Override
    public String key() {
        return "calculate_fees";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID transactionId = context.require(WalloopWorkflowContextKeys.TRANSACTION_ID, UUID.class);
        UUID ownerId = context.require(WalloopWorkflowContextKeys.OWNER_ID, UUID.class);

        long feeSats = feeCalculationService.calculateFeeSats(transactionId, ownerId);
        context.put(WalloopWorkflowContextKeys.FEE_SATS, feeSats);

        log.info("Fee calculated (sats)={} transactionId={}", feeSats, transactionId);
        return StepResult.completed("Fee calculated");
    }
}

