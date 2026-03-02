package io.walloop.engine.lightning.swap;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LightningSwapStatusScheduler {

    private final List<LightningSwapStatusPoller> pollers;
    private final TaskScheduler taskScheduler;

    @Value("${lightning.swap.status-cron:${boltz.status-cron:0 * * * * *}}")
    private String statusCron;

    @Value("${walloop.engine.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduled;

    @PostConstruct
    void startIfPending() {
        if (!schedulerEnabled) {
            return;
        }
        boolean hasPending = pollers.stream().anyMatch(LightningSwapStatusPoller::hasPending);
        if (hasPending) {
            ensurePolling();
        }
    }

    public void ensurePolling() {
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
            boolean hasPending = pollStatuses();
            if (!hasPending) {
                stopPolling();
            }
        } finally {
            running.set(false);
        }
    }

    boolean pollStatuses() {
        boolean pendingLeft = false;
        for (LightningSwapStatusPoller poller : pollers) {
            try {
                if (poller.poll()) {
                    pendingLeft = true;
                }
            } catch (Exception e) {
                pendingLeft = true;
                log.warn("LightningSwapStatusScheduler - poll failed - poller={}", poller.getClass().getSimpleName(), e);
            }
        }
        if (pendingLeft) {
            log.debug("LightningSwapStatusScheduler - pending swaps detected");
        }
        return pendingLeft;
    }

    private void stopPolling() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }
}

