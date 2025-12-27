package com.walloop.engine.messaging;

import com.walloop.engine.sideshift.SideShiftShiftEntity;
import com.walloop.engine.sideshift.SideShiftShiftRepository;
import com.walloop.engine.sideshift.SideShiftShiftStatus;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawCompletedConsumer {

    private final SideShiftShiftRepository shiftRepository;

    @RabbitListener(
            queues = WithdrawMessagingConfiguration.WITHDRAW_COMPLETED_QUEUE,
            containerFactory = TransactionEngineMessagingConfiguration.TX_ENGINE_LISTENER_CONTAINER_FACTORY
    )
    public void onWithdrawCompleted(WithdrawCompletedMessage message) {
        SideShiftShiftEntity shift = shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(message.processId())
                .orElse(null);
        if (shift == null) {
            log.warn("Withdraw completed for unknown processId={}", message.processId());
            return;
        }

        if (shift.getStatus() == SideShiftShiftStatus.SETTLED) {
            return;
        }

        shift.setWithdrawCompletedAt(OffsetDateTime.now());
        shift.setStatus(SideShiftShiftStatus.WITHDRAW_COMPLETED);
        shift.setUpdatedAt(OffsetDateTime.now());
        shiftRepository.save(shift);
        log.info("Withdraw completed for processId={}", message.processId());
    }
}
