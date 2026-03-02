package io.walloop.engine.lightning;

import java.time.OffsetDateTime;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lightningj.lnd.wrapper.ClientSideException;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.AddInvoiceResponse;
import org.lightningj.lnd.wrapper.message.Channel;
import org.lightningj.lnd.wrapper.message.Invoice;
import org.lightningj.lnd.wrapper.message.ListChannelsResponse;
import org.lightningj.lnd.wrapper.message.PayReq;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LightningOutboundLiquidityService {

    private static final String PROVIDER = "manual-invoice";

    private final SynchronousLndAPI lndApi;
    private final LightningOutboundLiquidityRequestRepository requestRepository;
    private final LightningOutboundLiquidityStatusScheduler statusScheduler;

    @Value("${walloop.lightning.outbound-liquidity.invoice-expiry-seconds:86400}")
    private long invoiceExpirySeconds;

    public OutboundLiquidityInvoiceResponse createInvoice(OutboundLiquidityInvoiceRequest request) {
        long requestedSats = resolveRequestedSats(request);
        Channel targetChannel = resolveLargestAvailableChannel(requestedSats);
        String memo = buildMemo(targetChannel, request);
        String paymentRequest = createInvoiceViaLnd(memo, requestedSats);
        String paymentHash = resolvePaymentHash(paymentRequest);

        LightningOutboundLiquidityRequestEntity entity = new LightningOutboundLiquidityRequestEntity();
        entity.setProvider(PROVIDER);
        entity.setTargetNodePubkey(targetChannel.getRemotePubkey());
        entity.setTargetChannelId(targetChannel.getChanId());
        entity.setTargetChannelCapacitySats(targetChannel.getCapacity());
        entity.setTargetChannelLocalBalanceSats(targetChannel.getLocalBalance());
        entity.setTargetChannelRemoteBalanceSats(targetChannel.getRemoteBalance());
        entity.setTargetChannelLocalReserveSats(targetChannel.getLocalChanReserveSat());
        entity.setTargetChannelCommitFeeSats(targetChannel.getCommitFee());
        entity.setTargetChannelSpendableSats(resolveSpendableSats(targetChannel));
        entity.setRequestedSats(requestedSats);
        entity.setInvoice(paymentRequest);
        entity.setPaymentHash(paymentHash);
        entity.setInvoiceMemo(memo);
        entity.setInvoiceExpirySeconds(invoiceExpirySeconds);
        entity.setStatus(LightningOutboundLiquidityRequestStatus.CREATED);
        entity.setPollAttempts(0);
        entity.setLastPolledAt(null);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(entity);
        statusScheduler.ensurePolling();

        log.info(
                "LightningOutboundLiquidityService - outbound liquidity invoice created - requestId={} channelId={} nodePubkey={} requestedSats={}",
                entity.getId(),
                entity.getTargetChannelId(),
                entity.getTargetNodePubkey(),
                entity.getRequestedSats()
        );

        return new OutboundLiquidityInvoiceResponse(
                entity.getId(),
                entity.getInvoice(),
                entity.getRequestedSats(),
                entity.getTargetNodePubkey(),
                entity.getTargetChannelId(),
                entity.getTargetChannelCapacitySats(),
                entity.getTargetChannelLocalBalanceSats(),
                entity.getTargetChannelRemoteBalanceSats(),
                entity.getTargetChannelSpendableSats(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }

    private String resolvePaymentHash(String paymentRequest) {
        try {
            PayReq payReq = lndApi.decodePayReq(paymentRequest);
            if (payReq == null || payReq.getPaymentHash() == null || payReq.getPaymentHash().isBlank()) {
                throw new IllegalStateException("Failed to resolve payment hash from LND invoice");
            }
            return payReq.getPaymentHash();
        } catch (StatusException | ValidationException e) {
            throw new IllegalStateException("Failed to decode outbound liquidity invoice", e);
        }
    }

    private String createInvoiceViaLnd(String memo, long requestedSats) {
        try {
            Invoice invoice = new Invoice();
            invoice.setMemo(memo);
            invoice.setValue(requestedSats);
            invoice.setExpiry(invoiceExpirySeconds);
            AddInvoiceResponse response = lndApi.addInvoice(invoice);
            String paymentRequest = response.getPaymentRequest();
            if (paymentRequest == null || paymentRequest.isBlank()) {
                throw new IllegalStateException("Lightning outbound liquidity invoice not returned by LND");
            }
            return paymentRequest;
        } catch (StatusException | ValidationException e) {
            throw new IllegalStateException("Failed to create outbound liquidity invoice in LND", e);
        }
    }

    private long resolveRequestedSats(OutboundLiquidityInvoiceRequest request) {
        if (request == null || request.amountSats() == null || request.amountSats() <= 0) {
            throw new IllegalArgumentException("amountSats must be greater than zero");
        }
        return request.amountSats();
    }

    private Channel resolveLargestAvailableChannel(long requestedSats) {
        ListChannelsResponse response;
        try {
            response = lndApi.listChannels(true, false, false, false, null, false);
        } catch (StatusException | ValidationException e) {
            throw new IllegalStateException("Failed to list lightning channels from LND", e);
        }

        try {
            return response.getChannels()
                    .stream()
                    .filter(Channel::getActive)
                    .filter(channel -> channel.getRemoteBalance() >= requestedSats)
                    .max(Comparator.comparingLong(Channel::getCapacity)
                            .thenComparingLong(Channel::getRemoteBalance)
                            .thenComparingLong(Channel::getLocalBalance))
                    .orElseThrow(() -> new IllegalStateException(
                            "No active channel available with enough remote balance for requested amount"));
        } catch (ClientSideException e) {
            throw new IllegalStateException("Failed to parse channel list from LND", e);
        }
    }

    private long resolveSpendableSats(Channel channel) {
        long spendable = channel.getLocalBalance() - channel.getLocalChanReserveSat() - channel.getCommitFee();
        return Math.max(0L, spendable);
    }

    private String buildMemo(Channel channel, OutboundLiquidityInvoiceRequest request) {
        String note = request.note() == null ? "" : request.note().trim();
        String base = "walloop:outbound-liquidity:" + channel.getChanId();
        if (note.isBlank()) {
            return base;
        }
        return base + ":" + note;
    }
}

