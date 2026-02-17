package com.walloop.engine.lightning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.message.Invoice;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

class LightningOutboundLiquidityStatusSchedulerTest {

    private LightningOutboundLiquidityRequestRepository repository;
    private SynchronousLndAPI lndApi;
    private TaskScheduler taskScheduler;
    private LightningOutboundLiquidityStatusScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LightningOutboundLiquidityRequestRepository.class);
        lndApi = Mockito.mock(SynchronousLndAPI.class);
        taskScheduler = Mockito.mock(TaskScheduler.class);
        scheduler = new LightningOutboundLiquidityStatusScheduler(repository, lndApi, taskScheduler);
        ReflectionTestUtils.setField(scheduler, "maxRetries", 5);
    }

    @Test
    void shouldMarkRequestAsPaidWhenInvoiceSettled() throws Exception {
        LightningOutboundLiquidityRequestEntity request = new LightningOutboundLiquidityRequestEntity();
        request.setId(UUID.randomUUID());
        request.setPaymentHash("hash-1");
        request.setStatus(LightningOutboundLiquidityRequestStatus.CREATED);
        request.setRequestedSats(20_000L);
        request.setTargetChannelId(123L);
        request.setCreatedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());

        when(repository.findByStatusIn(List.of(LightningOutboundLiquidityRequestStatus.CREATED)))
                .thenReturn(List.of(request));

        Invoice invoice = new Invoice();
        invoice.setSettled(true);
        when(lndApi.lookupInvoice(Mockito.any(org.lightningj.lnd.wrapper.message.PaymentHash.class)))
                .thenReturn(invoice);

        boolean pending = scheduler.poll();

        assertThat(pending).isFalse();
        ArgumentCaptor<LightningOutboundLiquidityRequestEntity> captor =
                ArgumentCaptor.forClass(LightningOutboundLiquidityRequestEntity.class);
        verify(repository).save(captor.capture());
        LightningOutboundLiquidityRequestEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(LightningOutboundLiquidityRequestStatus.PAID);
        assertThat(saved.getPaidAt()).isNotNull();
    }

    @Test
    void shouldKeepPendingWhenInvoiceNotSettled() throws Exception {
        LightningOutboundLiquidityRequestEntity request = new LightningOutboundLiquidityRequestEntity();
        request.setId(UUID.randomUUID());
        request.setPaymentHash("hash-2");
        request.setStatus(LightningOutboundLiquidityRequestStatus.CREATED);
        request.setPollAttempts(0);
        request.setCreatedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());

        when(repository.findByStatusIn(List.of(LightningOutboundLiquidityRequestStatus.CREATED)))
                .thenReturn(List.of(request));

        Invoice invoice = new Invoice();
        invoice.setSettled(false);
        when(lndApi.lookupInvoice(Mockito.any(org.lightningj.lnd.wrapper.message.PaymentHash.class)))
                .thenReturn(invoice);

        boolean pending = scheduler.poll();

        assertThat(pending).isTrue();
        ArgumentCaptor<LightningOutboundLiquidityRequestEntity> captor =
                ArgumentCaptor.forClass(LightningOutboundLiquidityRequestEntity.class);
        verify(repository).save(captor.capture());
        LightningOutboundLiquidityRequestEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(LightningOutboundLiquidityRequestStatus.CREATED);
        assertThat(saved.getPollAttempts()).isEqualTo(1);
    }

    @Test
    void shouldMarkFailedAfterMaxRetries() throws Exception {
        LightningOutboundLiquidityRequestEntity request = new LightningOutboundLiquidityRequestEntity();
        request.setId(UUID.randomUUID());
        request.setPaymentHash("hash-3");
        request.setStatus(LightningOutboundLiquidityRequestStatus.CREATED);
        request.setPollAttempts(4);
        request.setCreatedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());

        when(repository.findByStatusIn(List.of(LightningOutboundLiquidityRequestStatus.CREATED)))
                .thenReturn(List.of(request));

        Invoice invoice = new Invoice();
        invoice.setSettled(false);
        when(lndApi.lookupInvoice(Mockito.any(org.lightningj.lnd.wrapper.message.PaymentHash.class)))
                .thenReturn(invoice);

        boolean pending = scheduler.poll();

        assertThat(pending).isFalse();
        ArgumentCaptor<LightningOutboundLiquidityRequestEntity> captor =
                ArgumentCaptor.forClass(LightningOutboundLiquidityRequestEntity.class);
        verify(repository).save(captor.capture());
        LightningOutboundLiquidityRequestEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(LightningOutboundLiquidityRequestStatus.FAILED);
        assertThat(saved.getPollAttempts()).isEqualTo(5);
        assertThat(saved.getErrorMessage()).isEqualTo("Invoice not settled yet");
    }
}
