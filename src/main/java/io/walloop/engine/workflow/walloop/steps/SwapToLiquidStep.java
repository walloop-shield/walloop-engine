package io.walloop.engine.workflow.walloop.steps;

import io.walloop.engine.messaging.WithdrawRequestPublisher;
import io.walloop.engine.swap.SwapToLiquidPartner;
import io.walloop.engine.swap.SwapOrderEntity;
import io.walloop.engine.swap.SwapOrderRepository;
import io.walloop.engine.swap.SwapOrderStatus;
import io.walloop.engine.swap.SwapQuoteService;
import io.walloop.engine.swap.SwapToLiquidRequest;
import io.walloop.engine.swap.SwapToLiquidResult;
import io.walloop.engine.workflow.StepResult;
import io.walloop.engine.workflow.WorkflowContext;
import io.walloop.engine.workflow.WorkflowExecutionRepository;
import io.walloop.engine.workflow.StepStatus;
import io.walloop.engine.workflow.WorkflowStep;
import io.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
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

    private final SwapToLiquidPartner swapToLiquidPartner;
    private final SwapOrderRepository orderRepository;
    private final SwapQuoteService swapQuoteService;
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
        UUID ownerId = context.require(WalloopWorkflowContextKeys.OWNER_ID, UUID.class);
        String chain = context.require(WalloopWorkflowContextKeys.CHAIN, String.class);
        swapQuoteService.ensureQuote(processId, chain, chain);
        String liquidAddress = context.get(WalloopWorkflowContextKeys.LIQUID_ADDRESS, String.class)
                .orElseThrow(() -> new IllegalStateException("Liquid address not present in context"));
        String refundAddress = context.require(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, String.class);

        SwapOrderEntity shiftEntity = orderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElse(null);
        if (shiftEntity == null) {
            try {
                SwapToLiquidRequest request = new SwapToLiquidRequest(
                        chain,
                        chain,
                        liquidAddress,
                        refundAddress,
                        processId,
                        ownerId
                );
                SwapToLiquidResult shift = swapToLiquidPartner.createSwap(request);

                context.put(WalloopWorkflowContextKeys.SWAP_ID, shift.swapId());
                context.put(WalloopWorkflowContextKeys.SWAP_DEPOSIT_ADDRESS, shift.depositAddress());

                log.info(
                        "SwapToLiquidStep - swap created - processId={} depositCoin={} depositAddress={} settleCoin={} settleNetwork={}",
                        processId,
                        shift.depositCoin(),
                        shift.depositAddress(),
                        shift.settleCoin(),
                        shift.settleNetwork()
                );

                shiftEntity = orderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                        .orElse(null);
            } catch (RuntimeException e) {
                return retryOrFail(processId, "Swap partner creation failed", e);
            }

            if (shiftEntity == null) {
                return retryOrFail(processId, "Swap order not persisted", null);
            }
        }

        if (shiftEntity.getPartnerOrderId() != null) {
            context.put(WalloopWorkflowContextKeys.SWAP_ID, shiftEntity.getPartnerOrderId());
        }
        if (shiftEntity.getDepositAddress() != null) {
            context.put(WalloopWorkflowContextKeys.SWAP_DEPOSIT_ADDRESS, shiftEntity.getDepositAddress());
        }

        if (shiftEntity.getStatus() == SwapOrderStatus.SETTLED) {
            return StepResult.completed("Swap to Liquid settled");
        }

        if (shiftEntity.getWithdrawCompletedAt() == null) {
            if (shiftEntity.getWithdrawRequestedAt() == null) {
                withdrawRequestPublisher.publish(processId);
                shiftEntity.setWithdrawRequestedAt(OffsetDateTime.now());
                shiftEntity.setStatus(SwapOrderStatus.WITHDRAW_REQUESTED);
                shiftEntity.setUpdatedAt(OffsetDateTime.now());
                orderRepository.save(shiftEntity);
                log.info(
                        "SwapToLiquidStep - withdraw requested - processId={} destination={}",
                        processId,
                        shiftEntity.getPartner()
                );
            }
            return StepResult.waiting("Waiting for swap partner withdrawal confirmation");
        }

        return StepResult.waiting("Waiting for swap partner settlement");
    }

    private StepResult retryOrFail(UUID processId, String detail, RuntimeException error) {
        int retries = countRetries(processId);
        if (retries >= MAX_RETRIES) {
            log.warn(
                    "SwapToLiquidStep - retry limit reached - processId={} detail={} retries={}",
                    processId,
                    detail,
                    retries,
                    error
            );
            return StepResult.failed(detail + " after retries");
        }
        log.warn(
                "SwapToLiquidStep - retry scheduled - processId={} detail={} retry={} maxRetries={}",
                processId,
                detail,
                retries + 1,
                MAX_RETRIES,
                error
        );
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


