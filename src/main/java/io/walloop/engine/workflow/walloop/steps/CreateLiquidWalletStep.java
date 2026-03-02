package io.walloop.engine.workflow.walloop.steps;

import io.walloop.engine.liquid.entity.LiquidWalletEntity;
import io.walloop.engine.liquid.service.LiquidWalletService;
import io.walloop.engine.workflow.StepResult;
import io.walloop.engine.workflow.WorkflowContext;
import io.walloop.engine.workflow.WorkflowExecutionRepository;
import io.walloop.engine.workflow.StepStatus;
import io.walloop.engine.workflow.WorkflowStep;
import io.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateLiquidWalletStep implements WorkflowStep {

    private final LiquidWalletService liquidWalletService;
    private final WorkflowExecutionRepository executionRepository;

    private static final int MAX_RETRIES = 5;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    @Override
    public String key() {
        return "create_liquid_wallet";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);
        UUID ownerId = context.require(WalloopWorkflowContextKeys.OWNER_ID, UUID.class);
        try {
            LiquidWalletEntity wallet = liquidWalletService.createForTransaction(processId, ownerId);
            context.put(WalloopWorkflowContextKeys.LIQUID_ADDRESS, wallet.getAddress());
            log.info("CreateLiquidWalletStep - Liquid wallet ready for processId={} owner={} address={}", processId, ownerId, wallet.getAddress());
            return StepResult.completed("Liquid wallet ready");
        } catch (RuntimeException e) {
            return retryOrFail(processId, "Liquid wallet creation failed", e);
        }
    }

    private StepResult retryOrFail(UUID processId, String detail, RuntimeException error) {
        int retries = countRetries(processId);
        if (retries >= MAX_RETRIES) {
            log.warn("CreateLiquidWalletStep - {} after {} retries processId={}", detail, retries, processId, error);
            return StepResult.failed(detail + " after retries");
        }
        log.warn("CreateLiquidWalletStep - {} (retry {}/{}) processId={}", detail, retries + 1, MAX_RETRIES, processId, error);
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

