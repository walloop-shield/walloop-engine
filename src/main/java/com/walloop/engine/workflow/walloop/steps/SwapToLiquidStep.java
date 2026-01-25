package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.messaging.WithdrawRequestPublisher;
import com.walloop.engine.sideshift.SideShiftPairSimulationService;
import com.walloop.engine.sideshift.SideShiftShiftEntity;
import com.walloop.engine.sideshift.SideShiftShiftRepository;
import com.walloop.engine.sideshift.SideShiftShiftResponse;
import com.walloop.engine.sideshift.SideShiftShiftStatus;
import com.walloop.engine.sideshift.SideShiftSwapService;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.StepStatus;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SwapToLiquidStep implements WorkflowStep {

    private final SideShiftSwapService sideShiftSwapService;
    private final SideShiftShiftRepository shiftRepository;
    private final SideShiftPairSimulationService pairSimulationService;
    private final WithdrawRequestPublisher withdrawRequestPublisher;
    private final WorkflowExecutionRepository executionRepository;

    private static final int MAX_RETRIES = 5;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    @Override
    public String key() {
        return "swap_to_liquid";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);
        String chain = context.require(WalloopWorkflowContextKeys.CHAIN, String.class);
        pairSimulationService.ensureSimulation(processId, chain, chain);
        String liquidAddress = context.get(WalloopWorkflowContextKeys.LIQUID_ADDRESS, String.class)
                .orElseThrow(() -> new IllegalStateException("Liquid address not present in context"));
        String refundAddress = context.require(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, String.class);
        String sessionToken = context.get(WalloopWorkflowContextKeys.SESSION_TOKEN, String.class).orElse(null);

        SideShiftShiftEntity shiftEntity = shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElse(null);
        if (shiftEntity == null) {
            try {
                SideShiftShiftResponse shift = sideShiftSwapService.swapToLiquid(
                        chain,
                        chain,
                        liquidAddress,
                        refundAddress,
                        processId,
                        sessionToken
                );

                context.put(WalloopWorkflowContextKeys.SWAP_ID, shift.id());
                context.put(WalloopWorkflowContextKeys.SWAP_DEPOSIT_ADDRESS, shift.depositAddress());

                log.info(
                        "SwapToLiquidStep - SideShift swap created for processId={} depositCoin={} depositAddress={} settleCoin={} settleNetwork={}",
                        processId,
                        shift.depositCoin(),
                        shift.depositAddress(),
                        shift.settleCoin(),
                        shift.settleNetwork()
                );

                shiftEntity = shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                        .orElse(null);
            } catch (RuntimeException e) {
                return retryOrFail(processId, "SideShift swap creation failed", e);
            }

            if (shiftEntity == null) {
                return retryOrFail(processId, "SideShift shift not persisted", null);
            }
        }

        if (shiftEntity.getShiftId() != null) {
            context.put(WalloopWorkflowContextKeys.SWAP_ID, shiftEntity.getShiftId());
        }
        if (shiftEntity.getDepositAddress() != null) {
            context.put(WalloopWorkflowContextKeys.SWAP_DEPOSIT_ADDRESS, shiftEntity.getDepositAddress());
        }

        if (shiftEntity.getStatus() == SideShiftShiftStatus.SETTLED) {
            return StepResult.completed("Swap to Liquid settled");
        }

        if (shiftEntity.getWithdrawCompletedAt() == null) {
            if (shiftEntity.getWithdrawRequestedAt() == null) {
                withdrawRequestPublisher.publish(processId);
                shiftEntity.setWithdrawRequestedAt(OffsetDateTime.now());
                shiftEntity.setStatus(SideShiftShiftStatus.WITHDRAW_REQUESTED);
                shiftEntity.setUpdatedAt(OffsetDateTime.now());
                shiftRepository.save(shiftEntity);
                log.info("SwapToLiquidStep - Withdraw requested for processId={} destination=SIDESHIFT", processId);
            }
            return StepResult.waiting("Waiting for SideShift withdrawal confirmation");
        }

        return StepResult.waiting("Waiting for SideShift settlement");
    }

    private StepResult retryOrFail(UUID processId, String detail, RuntimeException error) {
        int retries = countRetries(processId);
        if (retries >= MAX_RETRIES) {
            log.warn("SwapToLiquidStep - {} after {} retries processId={}", detail, retries, processId, error);
            return StepResult.failed(detail + " after retries");
        }
        log.warn("SwapToLiquidStep - {} (retry {}/{}) processId={}", detail, retries + 1, MAX_RETRIES, processId, error);
        return StepResult.retry(detail, RETRY_DELAY);
    }

    private int countRetries(UUID processId) {
        return executionRepository.findByTransactionId(processId)
                .map(execution -> execution.getHistory().stream()
                        .filter(item -> key().equals(item.stepKey()))
                        .filter(item -> item.status() == StepStatus.RETRY)
                        .count())
                .map(Long::intValue)
                .orElse(0);
    }
}

