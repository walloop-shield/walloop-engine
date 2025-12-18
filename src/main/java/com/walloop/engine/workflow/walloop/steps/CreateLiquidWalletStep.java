package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CreateLiquidWalletStep implements WorkflowStep {

    @Override
    public String key() {
        return "create_liquid_wallet";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        String derivationPath = context.get(WalloopWorkflowContextKeys.DERIVATION_PATH, String.class).orElse(null);
        log.info("Creating Liquid wallet (placeholder) derivationPath={}", derivationPath);
        return StepResult.completed("Liquid wallet created (placeholder)");
    }
}

