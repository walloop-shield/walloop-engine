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
        log.info("Deposit assumed already detected for ownerId={}", context.get(WalloopWorkflowContextKeys.OWNER_ID).orElse(null));
        return StepResult.completed("Deposit detection handled externally");
    }
}
