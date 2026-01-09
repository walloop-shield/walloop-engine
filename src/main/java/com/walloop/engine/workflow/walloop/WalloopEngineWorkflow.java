package com.walloop.engine.workflow.walloop;

import com.walloop.engine.workflow.WorkflowDefinition;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.steps.AwaitWalloopDepositStep;
import com.walloop.engine.workflow.walloop.steps.CalculateFeesStep;
import com.walloop.engine.workflow.walloop.steps.CreateLightningInvoiceStep;
import com.walloop.engine.workflow.walloop.steps.CreateLiquidWalletStep;
import com.walloop.engine.workflow.walloop.steps.PayLiquidToLightningStep;
import com.walloop.engine.workflow.walloop.steps.ReturnToMainWalletStep;
import com.walloop.engine.workflow.walloop.steps.ConvertLightningToWalloopStep;
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
    private final CreateLightningInvoiceStep createLightningInvoiceStep;
    private final SwapToLiquidStep swapToLiquidStep;
    private final PayLiquidToLightningStep payLiquidToLightningStep;
    private final ConvertLightningToWalloopStep convertLightningToWalloopStep;
    private final ReturnToMainWalletStep returnToMainWalletStep;

    @Override
    public String name() {
        return "walloop_engine_workflow_v1";
    }

    @Override
    public List<WorkflowStep> steps() {
        return List.of(
                awaitWalloopDepositStep,
                createLiquidWalletStep,
                createLightningInvoiceStep,
                swapToLiquidStep,
                payLiquidToLightningStep,
                convertLightningToWalloopStep,
                returnToMainWalletStep,
                calculateFeesStep
        );
    }
}
