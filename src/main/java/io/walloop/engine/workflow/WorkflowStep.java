package io.walloop.engine.workflow;

public interface WorkflowStep {

    String key();

    StepResult execute(WorkflowContext context) throws Exception;
}


