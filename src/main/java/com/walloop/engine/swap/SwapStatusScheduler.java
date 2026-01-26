package com.walloop.engine.swap;

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
public class SwapStatusScheduler {

    private final SwapOrderRepository orderRepository;
    private final List<SwapStatusPoller> pollers;
    private final TaskScheduler taskScheduler;

    @Value("${swap.status-cron:${sideshift.status-cron:0 * * * * *}}")
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
        if (orderRepository.existsByStatusIsNullOrStatusNot(SwapOrderStatus.SETTLED)) {
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
        List<SwapOrderEntity> orders = orderRepository.findByStatusIsNullOrStatusNot(SwapOrderStatus.SETTLED);
        if (orders.isEmpty()) {
            return false;
        }

        Map<SwapPartner, SwapStatusPoller> pollerMap = new EnumMap<>(SwapPartner.class);
        for (SwapStatusPoller poller : pollers) {
            pollerMap.put(poller.partner(), poller);
        }

        boolean pendingLeft = false;
        for (SwapOrderEntity order : orders) {
            SwapPartner partner = order.getPartner();
            SwapStatusPoller poller = pollerMap.get(partner);
            if (poller == null) {
                pendingLeft = true;
                continue;
            }
        }

        Map<SwapPartner, List<SwapOrderEntity>> grouped = orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(SwapOrderEntity::getPartner));

        for (Map.Entry<SwapPartner, List<SwapOrderEntity>> entry : grouped.entrySet()) {
            SwapStatusPoller poller = pollerMap.get(entry.getKey());
            if (poller == null) {
                continue;
            }
            boolean partnerPending = poller.poll(entry.getValue());
            if (partnerPending) {
                pendingLeft = true;
            }
        }

        if (pendingLeft) {
            log.debug("SwapStatusScheduler - pending swaps detected - count={}", orders.size());
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
