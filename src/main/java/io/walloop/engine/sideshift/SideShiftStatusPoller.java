package io.walloop.engine.sideshift;

import io.walloop.engine.liquid.repository.LiquidWalletRepository;
import io.walloop.engine.swap.SwapOrderEntity;
import io.walloop.engine.swap.SwapOrderRepository;
import io.walloop.engine.swap.SwapOrderStatus;
import io.walloop.engine.swap.SwapPartner;
import io.walloop.engine.swap.SwapStatusPoller;
import io.walloop.engine.transaction.dto.WalletTransactionDetails;
import io.walloop.engine.transaction.service.WalletTransactionQueryService;
import io.walloop.engine.workflow.WorkflowContext;
import io.walloop.engine.workflow.WorkflowExecution;
import io.walloop.engine.workflow.WorkflowExecutionRepository;
import io.walloop.engine.workflow.WorkflowOrchestrator;
import io.walloop.engine.workflow.walloop.WalloopEngineWorkflow;
import io.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SideShiftStatusPoller implements SwapStatusPoller {

    private final SwapOrderRepository orderRepository;
    private final SideShiftClient client;
    private final SideShiftProperties properties;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WalletTransactionQueryService walletTransactionQueryService;
    private final LiquidWalletRepository liquidWalletRepository;
    private final WorkflowOrchestrator orchestrator;
    private final ObjectProvider<WalloopEngineWorkflow> workflowProvider;

    @Override
    public SwapPartner partner() {
        return SwapPartner.SIDESHIFT;
    }

    @Override
    public boolean poll(List<SwapOrderEntity> orders) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            log.debug("SideShiftStatusPoller - polling skipped - reason=missing_secret");
            return false;
        }

        boolean pendingLeft = false;
        for (SwapOrderEntity order : orders) {
            String shiftId = order.getPartnerOrderId();
            if (shiftId == null || shiftId.isBlank()) {
                pendingLeft = true;
                continue;
            }
            try {
                SideShiftShiftStatusResponse response = client.getShift(secret, order.getUserIp(), shiftId);
                if (isSettled(response)) {
                    markSettledAndResume(order);
                } else {
                    pendingLeft = true;
                }
            } catch (Exception e) {
                log.warn(
                        "SideShiftStatusPoller - status poll failed - processId={} swapId={}",
                        order.getProcessId(),
                        shiftId,
                        e
                );
                pendingLeft = true;
            }
        }
        return pendingLeft;
    }

    private boolean isSettled(SideShiftShiftStatusResponse response) {
        if (response == null) {
            return false;
        }
        if ("settled".equalsIgnoreCase(response.status())) {
            return true;
        }
        if (response.deposits() == null) {
            return false;
        }
        return response.deposits().stream()
                .anyMatch(deposit -> "settled".equalsIgnoreCase(deposit.status()));
    }

    private void markSettledAndResume(SwapOrderEntity order) {
        order.setStatus(SwapOrderStatus.SETTLED);
        order.setSettledAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);

        UUID processId = order.getProcessId();
        WorkflowExecution execution = workflowExecutionRepository.findByTransactionId(processId)
                .orElse(null);
        if (execution == null) {
            log.warn("SideShiftStatusPoller - workflow execution missing - processId={}", processId);
            return;
        }

        UUID ownerId = execution.getOwnerId();
        if (ownerId == null) {
            log.warn("SideShiftStatusPoller - ownerId missing - processId={}", processId);
            return;
        }

        WorkflowContext context = buildContext(processId, ownerId);
        WalloopEngineWorkflow workflow = workflowProvider.getObject();
        orchestrator.resume(execution.getId(), workflow, context);
        log.info(
                "SideShiftStatusPoller - workflow resumed - processId={} executionId={}",
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
        liquidWalletRepository.findFirstByTransactionIdOrderByCreatedAtDesc(processId)
                .ifPresent(wallet -> context.put(WalloopWorkflowContextKeys.LIQUID_ADDRESS, wallet.getAddress()));
        return context;
    }
}

