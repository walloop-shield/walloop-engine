package com.walloop.engine.lightning;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LspWebhookService {

    private static final String PROVIDER = "amboss-magma";
    private static final Set<String> ORDER_ID_KEYS = Set.of("orderId", "order_id", "id");
    private static final Set<String> STATUS_KEYS = Set.of("status", "payment_status");

    private final LightningInboundLiquidityRequestRepository requestRepository;

    public void handleAmbossWebhook(Map<String, Object> payload) {
        Map<String, Object> eventPayload = extractPayload(payload);
        String eventType = findFirstString(payload, Set.of("event_type"));

        Optional<LightningInboundLiquidityRequestEntity> request = requestRepository
                .findFirstByProviderAndStatusInOrderByCreatedAtDesc(
                        PROVIDER,
                        List.of(LightningInboundLiquidityRequestStatus.REQUESTED)
                );

        if (request.isEmpty()) {
            log.warn("Amboss webhook received but no matching request found. provider={}", PROVIDER);
            return;
        }

        LightningInboundLiquidityRequestEntity entity = request.get();
        applyEventStatus(entity, eventType, eventPayload);
        entity.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(entity);
    }

    private void applyEventStatus(LightningInboundLiquidityRequestEntity entity, String eventType, Map<String, Object> payload) {
        if (eventType != null && !eventType.isBlank()) {
            String normalized = eventType.trim().toUpperCase();
            if ("MAGMA".equals(normalized)) {
                String orderId = findFirstString(payload, ORDER_ID_KEYS);
                if (orderId != null && !orderId.isBlank()) {
                    entity.setExternalId(orderId);
                }
                entity.setStatus(LightningInboundLiquidityRequestStatus.CONFIRMED);
                return;
            }
            if ("OPENCHANNEL".equals(normalized)) {
                entity.setStatus(LightningInboundLiquidityRequestStatus.OPEN_CHANNEL);
                return;
            }
            if ("CLOSECHANNEL".equals(normalized)) {
                entity.setStatus(LightningInboundLiquidityRequestStatus.CLOSED_CHANNEL);
                return;
            }
        }

        String status = findFirstString(payload, STATUS_KEYS);
        if (status != null && isFailureStatus(status)) {
            entity.setStatus(LightningInboundLiquidityRequestStatus.FAILED);
        }
    }

    private Map<String, Object> extractPayload(Map<String, Object> payload) {
        Object eventPayload = payload != null ? payload.get("payload") : null;
        if (eventPayload instanceof Map<?, ?> map) {
            return castMap(map);
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private boolean isFailureStatus(String status) {
        String normalized = status.trim().toUpperCase();
        return normalized.contains("FAIL")
                || normalized.contains("CANCEL")
                || normalized.contains("REJECT")
                || normalized.contains("ERROR");
    }

    private String findFirstString(Map<String, Object> payload, Set<String> keys) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Set<String> orderedKeys = new LinkedHashSet<>(keys);
        for (String key : orderedKeys) {
            Object value = findValueByKey(payload, key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private Object findValueByKey(Object node, String key) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String entryKey = String.valueOf(entry.getKey());
                if (entryKey.equals(key)) {
                    return entry.getValue();
                }
                Object found = findValueByKey(entry.getValue(), key);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (node instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                Object found = findValueByKey(item, key);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
