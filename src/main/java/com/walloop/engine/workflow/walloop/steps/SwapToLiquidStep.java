package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.sideshift.SideShiftShiftResponse;
import com.walloop.engine.sideshift.SideShiftSwapService;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStatus;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SwapToLiquidStep implements WorkflowStep {

    private final SideShiftSwapService sideShiftSwapService;

    @Override
    public String key() {
        return "swap_to_liquid";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID transactionId = context.require(WalloopWorkflowContextKeys.TRANSACTION_ID, UUID.class);
        String chain = context.require(WalloopWorkflowContextKeys.CHAIN, String.class);
        String liquidAddress = context.get(WalloopWorkflowContextKeys.NEW_ADDRESS, String.class)
                .orElse(context.get(WalloopWorkflowContextKeys.LIQUID_ADDRESS, String.class)
                        .orElseThrow(() -> new IllegalStateException("Liquid address not present in context")));

        SideShiftShiftResponse shift = sideShiftSwapService.swapToLiquidUsdt(chain, chain, liquidAddress);

        context.put(WalloopWorkflowContextKeys.SWAP_ID, shift.id());
        context.put(WalloopWorkflowContextKeys.SWAP_DEPOSIT_ADDRESS, shift.depositAddress());

        log.info(
                "SideShift swap created for tx={} depositCoin={} depositAddress={} settleCoin={} settleNetwork={}",
                transactionId,
                shift.depositCoin(),
                shift.depositAddress(),
                shift.settleCoin(),
                shift.settleNetwork()
        );

        return StepResult.completed(WorkflowStatus.LIQUID_SWAP_COMPLETED.name());
    }
}

