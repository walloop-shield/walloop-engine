package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.messaging.WithdrawRequestPublisher;
import com.walloop.engine.sideshift.SideShiftShiftEntity;
import com.walloop.engine.sideshift.SideShiftShiftRepository;
import com.walloop.engine.sideshift.SideShiftShiftResponse;
import com.walloop.engine.sideshift.SideShiftShiftStatus;
import com.walloop.engine.sideshift.SideShiftSwapService;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStatus;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.Optional;
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
    private final WithdrawRequestPublisher withdrawRequestPublisher;

    @Override
    public String key() {
        return "swap_to_liquid";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);
        String chain = context.require(WalloopWorkflowContextKeys.CHAIN, String.class);
        String liquidAddress = context.get(WalloopWorkflowContextKeys.LIQUID_ADDRESS, String.class)
                .orElseThrow(() -> new IllegalStateException("Liquid address not present in context"));
        String refundAddress = context.require(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, String.class);
        String sessionToken = context.get(WalloopWorkflowContextKeys.SESSION_TOKEN, String.class).orElse(null);

        Optional<SideShiftShiftEntity> existingShift = shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId);
        SideShiftShiftEntity shiftEntity = existingShift.orElseGet(() -> {
            SideShiftShiftResponse shift = sideShiftSwapService.swapToLiquidUsdt(
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
                    "SideShift swap created for processId={} depositCoin={} depositAddress={} settleCoin={} settleNetwork={}",
                    processId,
                    shift.depositCoin(),
                    shift.depositAddress(),
                    shift.settleCoin(),
                    shift.settleNetwork()
            );

            return shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                    .orElseThrow(() -> new IllegalStateException("SideShift shift not persisted for processId=" + processId));
        });

        if (shiftEntity.getShiftId() != null) {
            context.put(WalloopWorkflowContextKeys.SWAP_ID, shiftEntity.getShiftId());
        }
        if (shiftEntity.getDepositAddress() != null) {
            context.put(WalloopWorkflowContextKeys.SWAP_DEPOSIT_ADDRESS, shiftEntity.getDepositAddress());
        }

        if (shiftEntity.getStatus() == SideShiftShiftStatus.SETTLED) {
            return StepResult.completed(WorkflowStatus.LIQUID_SWAP_COMPLETED.name());
        }

        if (shiftEntity.getWithdrawCompletedAt() == null) {
            if (shiftEntity.getWithdrawRequestedAt() == null) {
                withdrawRequestPublisher.publish(processId);
                shiftEntity.setWithdrawRequestedAt(OffsetDateTime.now());
                shiftEntity.setStatus(SideShiftShiftStatus.WITHDRAW_REQUESTED);
                shiftEntity.setUpdatedAt(OffsetDateTime.now());
                shiftRepository.save(shiftEntity);
                log.info("Withdraw requested for processId={} destination=SIDESHIFT", processId);
            }
            return StepResult.waiting("Waiting for withdraw completion");
        }

        return StepResult.waiting("Waiting for SideShift settlement");
    }
}

