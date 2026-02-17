package com.walloop.engine.lightning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.message.AddInvoiceResponse;
import org.lightningj.lnd.wrapper.message.Channel;
import org.lightningj.lnd.wrapper.message.ListChannelsResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class LightningOutboundLiquidityServiceTest {

    private SynchronousLndAPI lndApi;
    private LightningOutboundLiquidityRequestRepository repository;
    private LightningOutboundLiquidityStatusScheduler statusScheduler;
    private LightningOutboundLiquidityService service;

    @BeforeEach
    void setUp() {
        lndApi = Mockito.mock(SynchronousLndAPI.class);
        repository = Mockito.mock(LightningOutboundLiquidityRequestRepository.class);
        statusScheduler = Mockito.mock(LightningOutboundLiquidityStatusScheduler.class);
        service = new LightningOutboundLiquidityService(lndApi, repository, statusScheduler);
        ReflectionTestUtils.setField(service, "invoiceExpirySeconds", 3600L);
    }

    @Test
    void shouldCreateInvoiceUsingLargestAvailableChannel() throws Exception {
        Channel small = channel(10L, true, 100_000L, 80_000L, 30_000L, 1_000L, 300L, "node-small");
        Channel largest = channel(20L, true, 1_000_000L, 140_000L, 600_000L, 10_000L, 320L, "node-largest");
        Channel inactive = channel(30L, false, 2_000_000L, 10_000L, 1_000_000L, 1_000L, 200L, "node-inactive");

        ListChannelsResponse channelsResponse = new ListChannelsResponse();
        channelsResponse.setChannels(List.of(small, largest, inactive));
        when(lndApi.listChannels(true, false, false, false, null, false)).thenReturn(channelsResponse);

        AddInvoiceResponse addInvoiceResponse = new AddInvoiceResponse();
        addInvoiceResponse.setPaymentRequest("lnbc1testinvoice");
        when(lndApi.addInvoice(any())).thenReturn(addInvoiceResponse);
        org.lightningj.lnd.wrapper.message.PayReq payReq = new org.lightningj.lnd.wrapper.message.PayReq();
        payReq.setPaymentHash("hash123");
        when(lndApi.decodePayReq("lnbc1testinvoice")).thenReturn(payReq);

        OutboundLiquidityInvoiceResponse response = service.createInvoice(new OutboundLiquidityInvoiceRequest(120_000L, "boot"));

        assertThat(response.invoice()).isEqualTo("lnbc1testinvoice");
        assertThat(response.targetChannelId()).isEqualTo(20L);
        assertThat(response.targetNodePubkey()).isEqualTo("node-largest");
        assertThat(response.amountSats()).isEqualTo(120_000L);
        assertThat(response.targetChannelSpendableSats()).isEqualTo(129_680L);

        ArgumentCaptor<LightningOutboundLiquidityRequestEntity> entityCaptor =
                ArgumentCaptor.forClass(LightningOutboundLiquidityRequestEntity.class);
        verify(repository).save(entityCaptor.capture());
        LightningOutboundLiquidityRequestEntity saved = entityCaptor.getValue();
        assertThat(saved.getTargetChannelId()).isEqualTo(20L);
        assertThat(saved.getRequestedSats()).isEqualTo(120_000L);
        assertThat(saved.getInvoice()).isEqualTo("lnbc1testinvoice");
        assertThat(saved.getPaymentHash()).isEqualTo("hash123");
        assertThat(saved.getStatus()).isEqualTo(LightningOutboundLiquidityRequestStatus.CREATED);
        verify(statusScheduler).ensurePolling();
    }

    @Test
    void shouldFailWhenNoChannelSupportsRequestedAmount() throws Exception {
        Channel only = channel(10L, true, 100_000L, 50_000L, 5_000L, 1_000L, 300L, "node-small");
        ListChannelsResponse channelsResponse = new ListChannelsResponse();
        channelsResponse.setChannels(List.of(only));
        when(lndApi.listChannels(true, false, false, false, null, false)).thenReturn(channelsResponse);

        assertThatThrownBy(() -> service.createInvoice(new OutboundLiquidityInvoiceRequest(20_000L, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active channel available");
    }

    private Channel channel(
            long chanId,
            boolean active,
            long capacity,
            long localBalance,
            long remoteBalance,
            long reserve,
            long commitFee,
            String remotePubKey
    ) {
        Channel channel = new Channel();
        channel.setChanId(chanId);
        channel.setActive(active);
        channel.setCapacity(capacity);
        channel.setLocalBalance(localBalance);
        channel.setRemoteBalance(remoteBalance);
        channel.setLocalChanReserveSat(reserve);
        channel.setCommitFee(commitFee);
        channel.setRemotePubkey(remotePubKey);
        return channel;
    }
}
