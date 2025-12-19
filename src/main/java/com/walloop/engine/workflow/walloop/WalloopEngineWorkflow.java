package com.walloop.engine.workflow.walloop;

import com.walloop.engine.workflow.WorkflowDefinition;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.steps.AwaitWalloopDepositStep;
import com.walloop.engine.workflow.walloop.steps.CalculateFeesStep;
import com.walloop.engine.workflow.walloop.steps.CreateLightningWalletStep;
import com.walloop.engine.workflow.walloop.steps.CreateLiquidWalletStep;
import com.walloop.engine.workflow.walloop.steps.PayLiquidToLightningStep;
import com.walloop.engine.workflow.walloop.steps.SendToCorrelatedWalletStep;
import com.walloop.engine.workflow.walloop.steps.SwapToLiquidStep;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalloopEngineWorkflow implements WorkflowDefinition {

    private final AwaitWalloopDepositStep awaitWalloopDepositStep;
    private final CalculateFeesStep calculateFeesStep;
    private final CreateLiquidWalletStep createLiquidWalletStep;
    private final CreateLightningWalletStep createLightningWalletStep;
    private final SwapToLiquidStep swapToLiquidStep;
    private final PayLiquidToLightningStep payLiquidToLightningStep;
    private final SendToCorrelatedWalletStep sendToCorrelatedWalletStep;

    @Override
    public String name() {
        return "walloop_engine_workflow_v1";
    }

    @Override
    public List<WorkflowStep> steps() {
        return List.of(
                awaitWalloopDepositStep,
                calculateFeesStep,
                createLiquidWalletStep,
                createLightningWalletStep,
                swapToLiquidStep,
                payLiquidToLightningStep,
                sendToCorrelatedWalletStep
        );
    }
}
