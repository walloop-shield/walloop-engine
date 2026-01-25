package com.walloop.engine.workflow.walloop.steps;

import com.walloop.engine.deposit.DepositMonitorEntity;
import com.walloop.engine.deposit.DepositMonitorPublisher;
import com.walloop.engine.deposit.DepositMonitorRepository;
import com.walloop.engine.deposit.DepositMonitorStatus;
import com.walloop.engine.dto.DepositMonitorMessage;
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
public class AwaitWalloopDepositStep implements WorkflowStep {

    private final DepositMonitorRepository depositMonitorRepository;
    private final DepositMonitorPublisher depositMonitorPublisher;

    @Override
    public String key() {
        return "await_walloop_deposit";
    }

    @Override
    public StepResult execute(WorkflowContext context) {
        UUID processId = context.require(WalloopWorkflowContextKeys.PROCESS_ID, UUID.class);
        UUID ownerId = context.require(WalloopWorkflowContextKeys.OWNER_ID, UUID.class);
        String address = context.require(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, String.class);
        String network = context.require(WalloopWorkflowContextKeys.CHAIN, String.class);

        return depositMonitorRepository.findById(processId)
                .map(monitor -> {
                    if (monitor.getStatus() == DepositMonitorStatus.DETECTED) {
                        log.info("AwaitWalloopDepositStep - Deposit detected for processId={} ownerId={}", processId, ownerId);
                        return StepResult.completed("Deposit confirmed");
                    }
                    return StepResult.waiting("Waiting for deposit to appear on Walloop wallet");
                })
                .orElseGet(() -> {
                    DepositMonitorEntity monitor = new DepositMonitorEntity();
                    monitor.setProcessId(processId);
                    monitor.setOwnerId(ownerId);
                    monitor.setAddress(address);
                    monitor.setNetwork(network);
                    monitor.setStatus(DepositMonitorStatus.PENDING);
                    monitor.setCreatedAt(OffsetDateTime.now());
                    monitor.setUpdatedAt(OffsetDateTime.now());
                    depositMonitorRepository.save(monitor);

                    depositMonitorPublisher.publish(new DepositMonitorMessage(address, network, ownerId, processId));
                    log.info("AwaitWalloopDepositStep - Deposit monitor created for processId={} ownerId={} address={} network={}", processId, ownerId, address, network);
                    return StepResult.waiting("Deposit monitoring started");
                });
    }
}
