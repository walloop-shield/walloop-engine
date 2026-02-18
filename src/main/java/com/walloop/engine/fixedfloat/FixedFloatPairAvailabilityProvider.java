package com.walloop.engine.fixedfloat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.pairs.PairAvailabilityItem;
import com.walloop.engine.pairs.PairAvailabilityProvider;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedFloatPairAvailabilityProvider implements PairAvailabilityProvider {

    private static final String PARTNER = "FIXEDFLOAT";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ORDER_TYPE = "float";
    private static final String DIRECTION = "from";
    private static final String FROM_CCY = "BTCLN";
    private static final BigDecimal AVAILABILITY_AMOUNT = new BigDecimal("0.002");

    private final FixedFloatClient client;
    private final ObjectMapper objectMapper;
    private final FixedFloatProperties properties;
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
        String fromKey = FROM_CCY;
        String toKey = lightningToOnchainRateRepository.findLatestByFromAssetAndNetwork(fromKey, normalized)
                .map(LightningToOnchainRateEntity::getToAsset)
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
        if (toKey == null) {
            log.error("FixedFloatPairAvailabilityProvider - mapping missing - network={}", normalized);
            return Optional.of(new PairAvailabilityItem(PARTNER, fromKey, null, false, null, null));
        }
        toKey = toKey.trim().toUpperCase(Locale.ROOT);
        try {
            FixedFloatPriceRequest request = new FixedFloatPriceRequest(
                    ORDER_TYPE,
                    fromKey,
                    toKey,
                    DIRECTION,
                    AVAILABILITY_AMOUNT
            );
            String payload = objectMapper.writeValueAsString(request);
            FixedFloatResponse<Map<String, Object>> response = client.getPrice(
                    requireApiKey(),
                    sign(payload),
                    payload
            );
            boolean available = isAvailable(response);
            BigDecimal assetUsd = extractBtclnUsd(response);
            BigDecimal minUsd = toUsd(extractFromLimit(response, "min"), assetUsd);
            BigDecimal maxUsd = toUsd(extractFromLimit(response, "max"), assetUsd);
            return Optional.of(new PairAvailabilityItem(PARTNER, fromKey, toKey, available, minUsd, maxUsd));
        } catch (Exception e) {
            log.warn("FixedFloatPairAvailabilityProvider - availability check failed - from={} to={}", fromKey, toKey, e);
            return Optional.of(new PairAvailabilityItem(PARTNER, fromKey, toKey, false, null, null));
        }
    }

    private boolean isAvailable(FixedFloatResponse<Map<String, Object>> response) {
        if (response == null || response.data() == null) {
            return false;
        }
        Object errors = response.data().get("errors");
        return !hasAnyValue(errors);
    }

    private boolean hasAnyValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) > 0;
        }
        return true;
    }

    private BigDecimal extractFromLimit(FixedFloatResponse<Map<String, Object>> response, String field) {
        if (response == null || response.data() == null) {
            return null;
        }
        Object fromNode = response.data().get("from");
        if (!(fromNode instanceof Map<?, ?> fromMap)) {
            return null;
        }
        Object value = fromMap.get(field);
        return toBigDecimal(value);
    }

    private BigDecimal toUsd(BigDecimal fromLimit, BigDecimal assetUsd) {
        if (fromLimit == null || assetUsd == null) {
            return null;
        }
        if (fromLimit.compareTo(BigDecimal.ZERO) <= 0
                || assetUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return fromLimit.multiply(assetUsd).setScale(8, java.math.RoundingMode.DOWN);
    }

    private BigDecimal extractBtclnUsd(FixedFloatResponse<Map<String, Object>> response) {
        if (response == null || response.data() == null) {
            return null;
        }
        Object fromNode = response.data().get("from");
        if (!(fromNode instanceof Map<?, ?> fromMap)) {
            return null;
        }
        Object codeObj = fromMap.get("code");
        if (codeObj == null || !FROM_CCY.equalsIgnoreCase(codeObj.toString())) {
            return null;
        }
        BigDecimal amount = toBigDecimal(fromMap.get("amount"));
        BigDecimal usd = toBigDecimal(fromMap.get("usd"));
        if (amount == null || usd == null || amount.compareTo(BigDecimal.ZERO) <= 0 || usd.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return usd.divide(amount, 18, java.math.RoundingMode.DOWN);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private String requireApiKey() {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("FixedFloat API key not configured");
        }
        return apiKey;
    }

    private String sign(String payload) {
        String secret = properties.getApiSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("FixedFloat API secret not configured");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign FixedFloat payload", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
