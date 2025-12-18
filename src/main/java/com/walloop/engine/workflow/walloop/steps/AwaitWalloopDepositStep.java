package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AwaitWalloopDepositStep implements WorkflowStep {

    @Override
    public String key() {
        return "await_walloop_deposit";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        boolean deposited = context.get(WalloopWorkflowContextKeys.WALLOOP_DEPOSIT_DETECTED, Boolean.class)
                .orElse(false);

        if (!deposited) {
            return StepResult.waiting("Waiting for deposit to appear on Walloop wallet");
        }

        log.info("Deposit detected for ownerId={}", context.get(WalloopWorkflowContextKeys.OWNER_ID).orElse(null));
        return StepResult.completed("Deposit detected");
    }
}
