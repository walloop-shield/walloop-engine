package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.messaging.WithdrawRequestPublisher;
import com.walloop.engine.withdrawal.WalloopWithdrawalEntity;
import com.walloop.engine.withdrawal.WalloopWithdrawalRepository;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnToMainWalletStep implements WorkflowStep {

    private final WalloopWithdrawalRepository withdrawalRepository;
    private final WithdrawRequestPublisher withdrawRequestPublisher;

    @Override
    public String key() {
        return "return_to_main_wallet";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);
        String chain = context.require(WalloopWorkflowContextKeys.CHAIN, String.class);
        String correlatedAddress = context.require(WalloopWorkflowContextKeys.CORRELATED_ADDRESS, String.class);

        WalloopWithdrawalEntity withdrawal = withdrawalRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId)
                .orElseGet(() -> {
                    WalloopWithdrawalEntity entity = new WalloopWithdrawalEntity();
                    entity.setProcessId(processId);
                    entity.setChain(chain);
                    entity.setDestinationAddress(correlatedAddress);
                    entity.setCreatedAt(OffsetDateTime.now());
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return withdrawalRepository.save(entity);
                });

        if (withdrawal.getCompletedAt() != null) {
            return StepResult.completed("Funds returned to main wallet");
        }

        if (withdrawal.getRequestedAt() == null) {
            withdrawRequestPublisher.publish(processId, WithdrawRequestPublisher.DESTINATION_WALLOOP);
            withdrawal.setRequestedAt(OffsetDateTime.now());
            withdrawal.setUpdatedAt(OffsetDateTime.now());
            withdrawalRepository.save(withdrawal);
            log.info("Withdraw requested for processId={} destination=WALLOOP", processId);
        }

        return StepResult.waiting("Waiting for destination wallet withdrawal confirmation");
    }
}
