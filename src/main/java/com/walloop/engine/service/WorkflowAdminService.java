package com.walloop.engine.service;

import com.walloop.engine.liquid.repository.LiquidWalletRepository;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.WorkflowStatus;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowAdminService {

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowOrchestrator orchestrator;
    private final WalloopEngineWorkflow workflow;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final LiquidWalletRepository liquidWalletRepository;

    public WorkflowExecution start(UUID processId, UUID ownerId, String stepKey) {
        if (processId == null || ownerId == null || stepKey == null || stepKey.isBlank()) {
            throw new IllegalArgumentException("processId, ownerId and stepKey are required");
        }

        WorkflowExecution execution = executionRepository.findByTransactionId(processId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow execution not found for processId=" + processId));
        if (execution.getOwnerId() != null && !execution.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("OwnerId mismatch for processId=" + processId);
        }

        int stepIndex = resolveStepIndex(workflow.steps(), stepKey);
        execution.setNextStepIndex(stepIndex);
        execution.setStatus(WorkflowStatus.WAITING);
        execution.clearRetry();
        executionRepository.save(execution);

        WorkflowContext context = buildContext(processId, ownerId);
        WorkflowExecution resumed = orchestrator.resume(execution.getId(), workflow, context);
        log.info("WorkflowAdminService - Workflow resumed from step {} processId={} executionId={}", stepKey, processId, execution.getId());
        return resumed;
    }

    private WorkflowContext buildContext(UUID processId, UUID ownerId) {
        WalletTransactionDetails tx = walletTransactionQueryService.require(processId, ownerId);
        WorkflowContext context = new WorkflowContext();
        context.put(WalloopWorkflowContextKeys.PROCESS_ID, processId);
        context.put(WalloopWorkflowContextKeys.OWNER_ID, ownerId);
        context.put(WalloopWorkflowContextKeys.CHAIN, tx.chain());
        context.put(WalloopWorkflowContextKeys.CORRELATED_ADDRESS, tx.correlatedAddress());
        context.put(WalloopWorkflowContextKeys.DESTINATION_ADDRESS, tx.newAddress2());
        context.put(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, tx.newAddress());
        liquidWalletRepository.findFirstByTransactionIdOrderByCreatedAtDesc(processId)
                .ifPresent(wallet -> context.put(WalloopWorkflowContextKeys.LIQUID_ADDRESS, wallet.getAddress()));
        return context;
    }

    private int resolveStepIndex(List<WorkflowStep> steps, String stepKey) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).key().equals(stepKey)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Step not found in workflow: " + stepKey);
    }
}
