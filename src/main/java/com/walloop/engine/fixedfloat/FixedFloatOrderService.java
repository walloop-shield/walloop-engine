package com.walloop.engine.fixedfloat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.network.NetworkAssetService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FixedFloatOrderService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ORDER_TYPE = "float";
    private static final String DIRECTION = "from";
    private static final String FROM_CCY = "BTCLN";
    private static final int REQUIRED_CONFIRMATIONS = 1;

    private final FixedFloatClient client;
    private final FixedFloatProperties properties;
    private final FixedFloatOrderRepository repository;
    private final ObjectMapper objectMapper;
    private final NetworkAssetService networkAssetService;

    public FixedFloatOrderEntity createOrGetOrder(UUID processId, String chain, String toAddress, long amountSats) {
        Optional<FixedFloatOrderEntity> existing = repository.findFirstByProcessIdOrderByCreatedAtDesc(processId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String fromCcy = FROM_CCY;
        String toCcy = resolveToCcy(chain);
        BigDecimal amountBtc = BigDecimal.valueOf(amountSats)
                .movePointLeft(8)
                .setScale(8, RoundingMode.DOWN);

        FixedFloatCreateOrderRequest request = new FixedFloatCreateOrderRequest(
                ORDER_TYPE,
                fromCcy,
                toCcy,
                DIRECTION,
                amountBtc,
                toAddress,
                null,
                properties.getRefcode(),
                parseAfftax(properties.getAfftax())
        );
        String payload = toJson(request);
        FixedFloatResponse<Map<String, Object>> response = client.createOrder(
                requireApiKey(),
                sign(payload),
                payload
        );
        if (response == null) {
            throw new IllegalStateException("FixedFloat order response not available");
        }

        FixedFloatOrderEntity entity = new FixedFloatOrderEntity();
        entity.setProcessId(processId);
        entity.setOrderId(asString(nested(response.data(), "id")));
        entity.setOrderToken(asString(nested(response.data(), "token")));
        entity.setStatus(asString(nested(response.data(), "status")));
        entity.setFromCcy(fromCcy);
        entity.setToCcy(toCcy);
        entity.setAmount(amountBtc.stripTrailingZeros().toPlainString());
        entity.setToAddress(toAddress);
        entity.setConfirmations(asInteger(nested(response.data(), "from", "tx", "confirmations")));
        entity.setRequestPayload(payload);
        entity.setResponsePayload(toJson(response));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        if (!isSuccess(response)) {
            repository.save(entity);
            throw new IllegalStateException("FixedFloat order creation failed: " + response.msg());
        }

        if (entity.getOrderId() == null || entity.getOrderToken() == null) {
            repository.save(entity);
            throw new IllegalStateException("FixedFloat order response missing id/token");
        }

        repository.save(entity);
        return entity;
    }

    public FixedFloatOrderEntity refreshOrder(FixedFloatOrderEntity entity) {
        FixedFloatOrderRequest request = new FixedFloatOrderRequest(entity.getOrderId(), entity.getOrderToken());
        String payload = toJson(request);
        FixedFloatResponse<Map<String, Object>> response = client.getOrder(
                requireApiKey(),
                sign(payload),
                payload
        );
        if (response == null) {
            throw new IllegalStateException("FixedFloat order response not available");
        }

        entity.setStatus(asString(nested(response.data(), "status")));
        entity.setConfirmations(asInteger(nested(response.data(), "from", "tx", "confirmations")));
        entity.setResponsePayload(toJson(response));
        entity.setUpdatedAt(OffsetDateTime.now());

        if (isCompleted(entity)) {
            entity.setCompletedAt(OffsetDateTime.now());
        }

        return repository.save(entity);
    }

    public boolean isCompleted(FixedFloatOrderEntity entity) {
        Integer confirmations = entity.getConfirmations();
        int required = REQUIRED_CONFIRMATIONS;
        if (confirmations == null) {
            return false;
        }
        return confirmations >= required;
    }

    private String resolveToCcy(String chain) {
        String mainAsset = networkAssetService.requireMainAsset(chain);
        String normalizedChain = normalize(chain);
        String normalizedAsset = normalize(mainAsset);
        Map<String, String> overrides = properties.getToCcyOverrides();
        if (overrides != null) {
            String override = overrides.get(normalizedChain);
            if (override == null) {
                override = overrides.get(normalizedAsset);
            }
            if (override != null && !override.isBlank()) {
                return override;
            }
        }
        return mainAsset;
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

    private BigDecimal parseAfftax(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }

    private boolean isSuccess(FixedFloatResponse<?> response) {
        return response != null && "0".equals(response.code());
    }

    private Object nested(Map<String, Object> data, String... keys) {
        Object current = data;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace("_", "-");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize FixedFloat payload", e);
        }
    }
}
