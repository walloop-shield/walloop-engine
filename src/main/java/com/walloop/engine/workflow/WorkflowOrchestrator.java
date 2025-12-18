package com.walloop.engine.workflow;

import java.util.UUID;

public interface WorkflowOrchestrator {

    default WorkflowExecution start(WorkflowDefinition definition, WorkflowContext context) {
        return start(definition, context, null);
    }

    WorkflowExecution start(WorkflowDefinition definition, WorkflowContext context, WorkflowStartMetadata metadata);

    WorkflowExecution resume(UUID executionId, WorkflowDefinition definition, WorkflowContext context);
}
