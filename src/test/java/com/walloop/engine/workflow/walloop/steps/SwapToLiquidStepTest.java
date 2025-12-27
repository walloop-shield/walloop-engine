package com.walloop.engine.workflow.walloop.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.walloop.engine.messaging.WithdrawRequestPublisher;
import com.walloop.engine.sideshift.SideShiftShiftEntity;
import com.walloop.engine.sideshift.SideShiftShiftRepository;
import com.walloop.engine.sideshift.SideShiftShiftResponse;
import com.walloop.engine.sideshift.SideShiftShiftStatus;
import com.walloop.engine.sideshift.SideShiftSwapService;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.StepStatus;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SwapToLiquidStepTest {

    private SideShiftSwapService sideShiftSwapService;
    private SideShiftShiftRepository shiftRepository;
    private WithdrawRequestPublisher withdrawRequestPublisher;
    private SwapToLiquidStep step;

    @BeforeEach
    void setUp() {
        sideShiftSwapService = Mockito.mock(SideShiftSwapService.class);
        shiftRepository = Mockito.mock(SideShiftShiftRepository.class);
        withdrawRequestPublisher = Mockito.mock(WithdrawRequestPublisher.class);
        step = new SwapToLiquidStep(sideShiftSwapService, shiftRepository, withdrawRequestPublisher);
    }

    @Test
    void requestsWithdrawWhenShiftCreated() {
        UUID processId = UUID.randomUUID();
        WorkflowContext context = baseContext(processId);

        SideShiftShiftResponse response = new SideShiftShiftResponse(
                "shift-1",
                "deposit-addr",
                "btc",
                "bitcoin",
                "btc",
                "liquid"
        );

        SideShiftShiftEntity entity = new SideShiftShiftEntity();
        entity.setProcessId(processId);
        entity.setShiftId("shift-1");
        entity.setDepositAddress("deposit-addr");
        entity.setStatus(SideShiftShiftStatus.CREATED);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        when(shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId))
                .thenReturn(Optional.empty(), Optional.of(entity));
        when(sideShiftSwapService.swapToLiquid(
                eq("btc"),
                eq("btc"),
                eq("liquid-addr"),
                eq("tx-addr"),
                eq(processId),
                eq("session-token")
        ))
                .thenReturn(response);

        StepResult result = step.execute(context);

        assertThat(result.status()).isEqualTo(StepStatus.WAITING);
        verify(withdrawRequestPublisher).publish(processId);

        ArgumentCaptor<SideShiftShiftEntity> captor = ArgumentCaptor.forClass(SideShiftShiftEntity.class);
        verify(shiftRepository).save(captor.capture());
        SideShiftShiftEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SideShiftShiftStatus.WITHDRAW_REQUESTED);
        assertThat(saved.getWithdrawRequestedAt()).isNotNull();
    }

    @Test
    void completesWhenSettled() {
        UUID processId = UUID.randomUUID();
        WorkflowContext context = baseContext(processId);

        SideShiftShiftEntity entity = new SideShiftShiftEntity();
        entity.setProcessId(processId);
        entity.setStatus(SideShiftShiftStatus.SETTLED);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        when(shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId))
                .thenReturn(Optional.of(entity));

        StepResult result = step.execute(context);

        assertThat(result.status()).isEqualTo(StepStatus.COMPLETED);
        verify(withdrawRequestPublisher, never()).publish(any());
        verify(sideShiftSwapService, never()).swapToLiquid(any(), any(), any(), any(), any(), any());
    }

    @Test
    void doesNotRepeatWithdrawRequest() {
        UUID processId = UUID.randomUUID();
        WorkflowContext context = baseContext(processId);

        SideShiftShiftEntity entity = new SideShiftShiftEntity();
        entity.setProcessId(processId);
        entity.setStatus(SideShiftShiftStatus.WITHDRAW_REQUESTED);
        entity.setWithdrawRequestedAt(OffsetDateTime.now());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        when(shiftRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId))
                .thenReturn(Optional.of(entity));

        StepResult result = step.execute(context);

        assertThat(result.status()).isEqualTo(StepStatus.WAITING);
        verify(withdrawRequestPublisher, never()).publish(any());
        verify(shiftRepository, never()).save(any());
    }

    private WorkflowContext baseContext(UUID processId) {
        WorkflowContext context = new WorkflowContext();
        context.put(WalloopWorkflowContextKeys.PROCESS_ID, processId);
        context.put(WalloopWorkflowContextKeys.OWNER_ID, UUID.randomUUID());
        context.put(WalloopWorkflowContextKeys.CHAIN, "btc");
        context.put(WalloopWorkflowContextKeys.LIQUID_ADDRESS, "liquid-addr");
        context.put(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, "tx-addr");
        context.put(WalloopWorkflowContextKeys.SESSION_TOKEN, "session-token");
        return context;
    }
}
