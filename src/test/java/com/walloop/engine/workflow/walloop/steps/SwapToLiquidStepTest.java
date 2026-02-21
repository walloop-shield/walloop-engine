package com.walloop.engine.workflow.walloop.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.walloop.engine.messaging.WithdrawRequestPublisher;
import com.walloop.engine.swap.SwapOrderEntity;
import com.walloop.engine.swap.SwapOrderRepository;
import com.walloop.engine.swap.SwapOrderStatus;
import com.walloop.engine.swap.SwapPartner;
import com.walloop.engine.swap.SwapQuoteService;
import com.walloop.engine.swap.SwapToLiquidPartner;
import com.walloop.engine.swap.SwapToLiquidResult;
import com.walloop.engine.workflow.StepResult;
import com.walloop.engine.workflow.StepStatus;
import com.walloop.engine.workflow.WorkflowContext;
import com.walloop.engine.workflow.WorkflowExecutionRepository;
import com.walloop.engine.workflow.walloop.WalloopWorkflowContextKeys;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SwapToLiquidStepTest {

    private SwapToLiquidPartner swapToLiquidPartner;
    private SwapOrderRepository orderRepository;
    private SwapQuoteService swapQuoteService;
    private WithdrawRequestPublisher withdrawRequestPublisher;
    private WorkflowExecutionRepository executionRepository;
    private SwapToLiquidStep step;

    @BeforeEach
    void setUp() {
        swapToLiquidPartner = Mockito.mock(SwapToLiquidPartner.class);
        orderRepository = Mockito.mock(SwapOrderRepository.class);
        swapQuoteService = Mockito.mock(SwapQuoteService.class);
        withdrawRequestPublisher = Mockito.mock(WithdrawRequestPublisher.class);
        executionRepository = Mockito.mock(WorkflowExecutionRepository.class);
        step = new SwapToLiquidStep(
                swapToLiquidPartner,
                orderRepository,
                swapQuoteService,
                withdrawRequestPublisher,
                executionRepository
        );
    }

    @Test
    void requestsWithdrawWhenShiftCreated() {
        UUID processId = UUID.randomUUID();
        WorkflowContext context = baseContext(processId);

        SwapToLiquidResult response = new SwapToLiquidResult(
                "shift-1",
                "deposit-addr",
                "btc",
                "bitcoin",
                "btc",
                "liquid"
        );

        SwapOrderEntity entity = new SwapOrderEntity();
        entity.setProcessId(processId);
        entity.setPartner(SwapPartner.SIDESHIFT);
        entity.setPartnerOrderId("shift-1");
        entity.setDepositAddress("deposit-addr");
        entity.setStatus(SwapOrderStatus.CREATED);
        entity.setRequestPayload("{}");
        entity.setResponsePayload("{}");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        when(orderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId))
                .thenReturn(Optional.empty(), Optional.of(entity));
        when(swapToLiquidPartner.createSwap(any()))
                .thenReturn(response);

        StepResult result = step.execute(context);

        assertThat(result.status()).isEqualTo(StepStatus.WAITING);
        verify(swapQuoteService).ensureQuote(processId, "btc", "btc");
        verify(withdrawRequestPublisher).publish(processId);

        ArgumentCaptor<SwapOrderEntity> captor = ArgumentCaptor.forClass(SwapOrderEntity.class);
        verify(orderRepository).save(captor.capture());
        SwapOrderEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SwapOrderStatus.WITHDRAW_REQUESTED);
        assertThat(saved.getWithdrawRequestedAt()).isNotNull();
    }

    @Test
    void completesWhenSettled() {
        UUID processId = UUID.randomUUID();
        WorkflowContext context = baseContext(processId);

        SwapOrderEntity entity = new SwapOrderEntity();
        entity.setProcessId(processId);
        entity.setPartner(SwapPartner.SIDESHIFT);
        entity.setStatus(SwapOrderStatus.SETTLED);
        entity.setRequestPayload("{}");
        entity.setResponsePayload("{}");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        when(orderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId))
                .thenReturn(Optional.of(entity));

        StepResult result = step.execute(context);

        assertThat(result.status()).isEqualTo(StepStatus.COMPLETED);
        verify(swapQuoteService).ensureQuote(processId, "btc", "btc");
        verify(withdrawRequestPublisher, never()).publish(any());
        verify(swapToLiquidPartner, never()).createSwap(any());
    }

    @Test
    void doesNotRepeatWithdrawRequest() {
        UUID processId = UUID.randomUUID();
        WorkflowContext context = baseContext(processId);

        SwapOrderEntity entity = new SwapOrderEntity();
        entity.setProcessId(processId);
        entity.setPartner(SwapPartner.SIDESHIFT);
        entity.setStatus(SwapOrderStatus.WITHDRAW_REQUESTED);
        entity.setWithdrawRequestedAt(OffsetDateTime.now());
        entity.setRequestPayload("{}");
        entity.setResponsePayload("{}");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        when(orderRepository.findFirstByProcessIdOrderByCreatedAtDesc(processId))
                .thenReturn(Optional.of(entity));

        StepResult result = step.execute(context);

        assertThat(result.status()).isEqualTo(StepStatus.WAITING);
        verify(swapQuoteService).ensureQuote(processId, "btc", "btc");
        verify(withdrawRequestPublisher, never()).publish(any());
        verify(orderRepository, never()).save(any());
    }

    private WorkflowContext baseContext(UUID processId) {
        WorkflowContext context = new WorkflowContext();
        context.put(WalloopWorkflowContextKeys.PROCESS_ID, processId);
        context.put(WalloopWorkflowContextKeys.OWNER_ID, UUID.randomUUID());
        context.put(WalloopWorkflowContextKeys.CHAIN, "btc");
        context.put(WalloopWorkflowContextKeys.LIQUID_ADDRESS, "liquid-addr");
        context.put(WalloopWorkflowContextKeys.TRANSACTION_ADDRESS, "tx-addr");
        return context;
    }
}
