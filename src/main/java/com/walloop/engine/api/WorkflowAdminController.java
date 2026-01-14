package com.walloop.engine.api;

import com.walloop.engine.service.WorkflowAdminService;
import com.walloop.engine.workflow.WorkflowExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workflows")
public class WorkflowAdminController {

    private final WorkflowAdminService workflowAdminService;

    @PostMapping("/start")
    public StartWorkflowFromStepResponse start(@RequestBody StartWorkflowFromStepRequest request) {
        WorkflowExecution execution = workflowAdminService.start(
                request.processId(),
                request.ownerId(),
                request.stepKey()
        );
        return new StartWorkflowFromStepResponse(
                execution.getId(),
                execution.getWorkflowName(),
                execution.getStatus().name(),
                execution.getNextStepIndex(),
                request.stepKey()
        );
    }
}
