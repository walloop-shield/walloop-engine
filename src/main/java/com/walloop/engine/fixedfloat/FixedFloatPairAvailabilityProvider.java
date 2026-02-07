package com.walloop.engine.fixedfloat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.pairs.PairAvailabilityItem;
import com.walloop.engine.pairs.PairAvailabilityProvider;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedFloatPairAvailabilityProvider implements PairAvailabilityProvider {

    private static final String PARTNER = "FIXEDFLOAT";

    private final FixedFloatClient client;
    private final ObjectMapper objectMapper;
    private final LightningToOnchainRateRepository lightningToOnchainRateRepository;

    @Override
    public String partner() {
        return PARTNER;
    }

    @Override
    public Optional<PairAvailabilityItem> checkAvailability(String network) {
        if (network == null || network.isBlank()) {
            return Optional.empty();
        }
        String normalized = network.trim().toLowerCase(Locale.ROOT);
        String fromKey = "BTCLN";
        String toKey = lightningToOnchainRateRepository.findLatestByFromAssetAndNetwork(fromKey, normalized)
                .map(LightningToOnchainRateEntity::getToAsset)
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
        if (toKey == null) {
            log.warn("FixedFloatPairAvailabilityProvider - mapping missing - network={}", normalized);
            return Optional.of(new PairAvailabilityItem(PARTNER, fromKey, null, false));
        }
        toKey = toKey.trim().toUpperCase(Locale.ROOT);
        try {
            String payload = client.getPairs();
            boolean available = isAvailable(payload, fromKey, toKey);
            return Optional.of(new PairAvailabilityItem(PARTNER, fromKey, toKey, available));
        } catch (Exception e) {
            log.warn("FixedFloatPairAvailabilityProvider - availability check failed - from={} to={}", fromKey, toKey, e);
            return Optional.of(new PairAvailabilityItem(PARTNER, fromKey, toKey, false));
        }
    }

    private boolean isAvailable(String payload, String from, String to) throws Exception {
        if (payload == null || payload.isBlank()) {
            return false;
        }
        JsonNode root = objectMapper.readTree(payload);
        JsonNode node = root;
        if (node.has("result")) {
            node = node.get("result");
        }
        if (node.isObject()) {
            JsonNode fromNode = findKey(node, from);
            if (fromNode == null || fromNode.isNull()) {
                return false;
            }
            if (fromNode.isObject()) {
                JsonNode toNode = findKey(fromNode, to);
                return toNode != null && !toNode.isNull();
            }
            if (fromNode.isArray()) {
                for (JsonNode item : fromNode) {
                    if (item.isTextual() && to.equalsIgnoreCase(item.asText())) {
                        return true;
                    }
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    String value = item.asText();
                    if (value.equalsIgnoreCase(from + to) || value.equalsIgnoreCase(from + "_" + to)) {
                        return true;
                    }
                } else if (item.isObject()) {
                    JsonNode fromNode = findKey(item, from);
                    JsonNode toNode = findKey(item, to);
                    if (fromNode != null && toNode != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private JsonNode findKey(JsonNode node, String key) {
        if (node.has(key)) {
            return node.get(key);
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (node.has(lower)) {
            return node.get(lower);
        }
        String upper = key.toUpperCase(Locale.ROOT);
        if (node.has(upper)) {
            return node.get(upper);
        }
        return null;
    }
}
