package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SendToCorrelatedWalletStep implements WorkflowStep {

    @Override
    public String key() {
        return "send_to_correlated_wallet";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        log.info("Sending LN balance to correlated wallet (placeholder)");
        return StepResult.completed("Funds sent to correlated wallet (placeholder)");
    }
}

