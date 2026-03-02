package io.walloop.engine.workflow;

import java.util.List;

public interface WorkflowDefinition {

    String name();

    List<WorkflowStep> steps();
}


