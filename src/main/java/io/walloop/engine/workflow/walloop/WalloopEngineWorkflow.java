package io.walloop.engine.workflow.walloop;

import io.walloop.engine.workflow.WorkflowDefinition;
import io.walloop.engine.workflow.WorkflowStep;
import io.walloop.engine.workflow.walloop.steps.AwaitWalloopDepositStep;
import io.walloop.engine.workflow.walloop.steps.CalculateFeesStep;
import io.walloop.engine.workflow.walloop.steps.CreateLightningInvoiceStep;
import io.walloop.engine.workflow.walloop.steps.CreateLiquidWalletStep;
import io.walloop.engine.workflow.walloop.steps.PayLiquidToLightningStep;
import io.walloop.engine.workflow.walloop.steps.ReturnToMainWalletStep;
import io.walloop.engine.workflow.walloop.steps.ConvertLightningToWalloopStep;
import io.walloop.engine.workflow.walloop.steps.SwapToLiquidStep;
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
                swapToLiquidStep,
                createLightningInvoiceStep,
                payLiquidToLightningStep,
                convertLightningToWalloopStep,
                returnToMainWalletStep,
                calculateFeesStep
        );
    }
}

