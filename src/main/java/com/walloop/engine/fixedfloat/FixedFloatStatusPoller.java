package com.walloop.engine.fixedfloat;

import com.walloop.engine.conversion.ConversionOrderEntity;
import com.walloop.engine.conversion.ConversionPartner;
import com.walloop.engine.conversion.ConversionStatusPoller;
import com.walloop.engine.transaction.dto.WalletTransactionDetails;
import com.walloop.engine.transaction.service.WalletTransactionQueryService;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecution;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.WorkflowOrchestrator;
import com.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedFloatStatusPoller implements ConversionStatusPoller {

    private final FixedFloatOrderService orderService;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final WorkflowOrchestrator orchestrator;
    private final ObjectProvider<WalloopEngineWorkflow> workflowProvider;

    @Override
    public ConversionPartner partner() {
        return ConversionPartner.FIXEDFLOAT;
    }

    @Override
    public boolean poll(List<ConversionOrderEntity> orders) {
        boolean pendingLeft = false;
        for (ConversionOrderEntity order : orders) {
            try {
                ConversionOrderEntity updated = orderService.refreshOrder(order);
                if (updated.getCompletedAt() != null) {
                    resumeWorkflow(updated.getProcessId());
                } else {
                    pendingLeft = true;
                }
            } catch (Exception e) {
                log.warn(
                        "FixedFloatStatusPoller - status poll failed - orderId={}",
                        order.getPartnerOrderId(),
                        e
                );
                pendingLeft = true;
            }
        }
        return pendingLeft;
    }

    private void resumeWorkflow(UUID processId) {
        WorkflowExecution execution = workflowExecutionRepository.findByTransactionId(processId)
                .orElse(null);
        if (execution == null) {
            log.warn("FixedFloatStatusPoller - workflow execution missing - processId={}", processId);
            return;
        }
        UUID ownerId = execution.getOwnerId();
        if (ownerId == null) {
            log.warn("FixedFloatStatusPoller - ownerId missing - processId={}", processId);
            return;
        }

        WorkflowContext context = buildContext(processId, ownerId);
        WalloopEngineWorkflow workflow = workflowProvider.getObject();
        orchestrator.resume(execution.getId(), workflow, context);
        log.info(
                "FixedFloatStatusPoller - workflow resumed - processId={} executionId={}",
                processId,
                execution.getId()
        );
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
        return context;
    }
}
