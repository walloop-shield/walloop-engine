package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.liquid.entity.LiquidWalletEntity;
import com.walloop.engine.liquid.service.LiquidWalletService;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowStatus;
import com.walloop.engine.workflow.WorkflowStep;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateLiquidWalletStep implements WorkflowStep {

    private final LiquidWalletService liquidWalletService;

    @Override
    public String key() {
        return "create_liquid_wallet";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID transactionId = context.require(WalloopWorkflowContextKeys.TRANSACTION_ID, UUID.class);
        UUID ownerId = context.require(WalloopWorkflowContextKeys.OWNER_ID, UUID.class);
        LiquidWalletEntity wallet = liquidWalletService.createForTransaction(transactionId, ownerId);
        context.put(WalloopWorkflowContextKeys.LIQUID_ADDRESS, wallet.getAddress());
        log.info("Liquid wallet created for tx={} owner={} address={}", transactionId, ownerId, wallet.getAddress());
        return StepResult.completed(WorkflowStatus.WALLET_LIQUID_COMPLETED.name());
    }
}

