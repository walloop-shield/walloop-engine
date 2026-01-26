package com.walloop.engine.conversion;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
public class ConversionStatusScheduler {

    private final ConversionOrderRepository orderRepository;
    private final List<ConversionStatusPoller> pollers;
    private final TaskScheduler taskScheduler;

    @Value("${conversion.status-cron:${fixedfloat.status-cron:0 * * * * *}}")
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
        if (orderRepository.findByCompletedAtIsNull().stream().anyMatch(order -> order.getPartner() != null)) {
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
            boolean hasPending = pollOrders();
            if (!hasPending) {
                stopPolling();
            }
        } finally {
            running.set(false);
        }
    }

    boolean pollOrders() {
        List<ConversionOrderEntity> orders = orderRepository.findByCompletedAtIsNull();
        if (orders.isEmpty()) {
            return false;
        }

        Map<ConversionPartner, ConversionStatusPoller> pollerMap = new EnumMap<>(ConversionPartner.class);
        for (ConversionStatusPoller poller : pollers) {
            pollerMap.put(poller.partner(), poller);
        }

        Map<ConversionPartner, List<ConversionOrderEntity>> grouped = orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(ConversionOrderEntity::getPartner));

        boolean pendingLeft = false;
        for (Map.Entry<ConversionPartner, List<ConversionOrderEntity>> entry : grouped.entrySet()) {
            ConversionStatusPoller poller = pollerMap.get(entry.getKey());
            if (poller == null) {
                pendingLeft = true;
                continue;
            }
            boolean partnerPending = poller.poll(entry.getValue());
            if (partnerPending) {
                pendingLeft = true;
            }
        }

        if (pendingLeft) {
            log.debug("ConversionStatusScheduler - pending conversions detected - count={}", orders.size());
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
