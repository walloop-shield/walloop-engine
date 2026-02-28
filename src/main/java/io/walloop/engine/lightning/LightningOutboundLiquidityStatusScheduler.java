package io.walloop.engine.lightning;

import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.Invoice;
import org.lightningj.lnd.wrapper.message.PaymentHash;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LightningOutboundLiquidityStatusScheduler {

    private static final List<LightningOutboundLiquidityRequestStatus> PENDING_STATUSES =
            List.of(LightningOutboundLiquidityRequestStatus.CREATED);

    private final LightningOutboundLiquidityRequestRepository requestRepository;
    private final SynchronousLndAPI lndApi;
    private final TaskScheduler taskScheduler;

    @Value("${walloop.lightning.outbound-liquidity.status-cron:0 */5 * * * *}")
    private String statusCron;

    @Value("${walloop.engine.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${walloop.lightning.outbound-liquidity.max-retries:5}")
    private int maxRetries;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduled;

    @PostConstruct
    void startIfPending() {
        if (!schedulerEnabled) {
            return;
        }
        if (requestRepository.existsByStatusIn(PENDING_STATUSES)) {
            ensurePolling();
        }
    }

    public void ensurePolling() {
        if (!schedulerEnabled) {
            return;
        }
        if (scheduled != null && !scheduled.isCancelled()) {
            return;
        }
        scheduled = taskScheduler.schedule(this::pollSafely, new CronTrigger(statusCron));
    }

    void pollSafely() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            boolean hasPending = poll();
            if (!hasPending) {
                stopPolling();
            }
        } finally {
            running.set(false);
        }
    }

    boolean poll() {
        List<LightningOutboundLiquidityRequestEntity> requests = requestRepository.findByStatusIn(PENDING_STATUSES);
        if (requests.isEmpty()) {
            return false;
        }

        boolean pendingLeft = false;
        for (LightningOutboundLiquidityRequestEntity request : requests) {
            LookupResult lookup = lookupInvoice(request);
            if (lookup.settled()) {
                request.setStatus(LightningOutboundLiquidityRequestStatus.PAID);
                request.setPaidAt(OffsetDateTime.now());
                request.setUpdatedAt(OffsetDateTime.now());
                requestRepository.save(request);
                log.info(
                        "LightningOutboundLiquidityStatusScheduler - outbound liquidity request paid - requestId={} channelId={} requestedSats={}",
                        request.getId(),
                        request.getTargetChannelId(),
                        request.getRequestedSats()
                );
            } else {
                int attempts = request.getPollAttempts() + 1;
                request.setPollAttempts(attempts);
                request.setLastPolledAt(OffsetDateTime.now());
                request.setUpdatedAt(OffsetDateTime.now());
                if (attempts >= maxRetries) {
                    request.setStatus(LightningOutboundLiquidityRequestStatus.FAILED);
                    request.setErrorMessage(lookup.errorMessage());
                    requestRepository.save(request);
                    log.warn(
                            "LightningOutboundLiquidityStatusScheduler - outbound liquidity request failed after retries - requestId={} attempts={} error={}",
                            request.getId(),
                            attempts,
                            lookup.errorMessage()
                    );
                } else {
                    request.setErrorMessage(lookup.errorMessage());
                    requestRepository.save(request);
                    pendingLeft = true;
                }
            }
        }
        return pendingLeft;
    }

    private LookupResult lookupInvoice(LightningOutboundLiquidityRequestEntity request) {
        try {
            PaymentHash paymentHash = new PaymentHash();
            paymentHash.setRHashStr(request.getPaymentHash());
            Invoice invoice = lndApi.lookupInvoice(paymentHash);
            if (invoice == null) {
                return new LookupResult(false, "Invoice not found in LND");
            }
            if (invoice.getSettled()) {
                return new LookupResult(true, null);
            }
            return new LookupResult(false, "Invoice not settled yet");
        } catch (StatusException | ValidationException e) {
            log.warn(
                    "LightningOutboundLiquidityStatusScheduler - outbound liquidity lookup failed - requestId={} paymentHash={}",
                    request.getId(),
                    request.getPaymentHash(),
                    e
            );
            return new LookupResult(false, e.getMessage());
        } catch (Exception e) {
            log.warn(
                    "LightningOutboundLiquidityStatusScheduler - outbound liquidity lookup unexpected failure - requestId={}",
                    request.getId(),
                    e
            );
            return new LookupResult(false, e.getMessage());
        }
    }

    private void stopPolling() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }

    private record LookupResult(boolean settled, String errorMessage) {
    }
}

