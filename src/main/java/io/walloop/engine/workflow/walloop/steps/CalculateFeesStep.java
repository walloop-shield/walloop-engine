package io.walloop.engine.workflow.walloop.steps;

import io.walloop.engine.service.FeeCalculationService;
import io.walloop.engine.settlement.ProcessSettlementSnapshotService;
import io.walloop.engine.workflow.StepResult;
import io.walloop.engine.workflow.WorkflowContext;
import io.walloop.engine.workflow.WorkflowStep;
import io.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateFeesStep implements WorkflowStep {

    private final FeeCalculationService feeCalculationService;
    private final ProcessSettlementSnapshotService settlementSnapshotService;

    @Override
    public String key() {
        return "calculate_fees";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);
        UUID ownerId = context.require(WalloopWorkflowContextKeys.OWNER_ID, UUID.class);

        settlementSnapshotService.capture(processId);
        long fee = feeCalculationService.calculateFee(processId, ownerId);
        context.put(WalloopWorkflowContextKeys.FEE, fee);

        log.info("CalculateFeesStep - Fee calculated (sats)={} processId={}", fee, processId);
        return StepResult.completed("Fees calculated");
    }
}


