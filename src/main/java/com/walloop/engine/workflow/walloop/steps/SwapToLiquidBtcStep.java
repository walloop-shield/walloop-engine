package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SwapToLiquidBtcStep implements WorkflowStep {

    @Override
    public String key() {
        return "swap_to_liquid_btc";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        log.info("Swapping tokens to BTC on Liquid via sideshift.ai (placeholder)");
        return StepResult.completed("Swap completed (placeholder)");
    }
}

