package com.walloop.engine.fixedfloat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.conversion.ConversionOrderEntity;
import com.walloop.engine.conversion.ConversionOrderRepository;
import com.walloop.engine.conversion.ConversionPartner;
import com.walloop.engine.conversion.ConversionPartnerService;
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
public class FixedFloatOrderService implements ConversionPartnerService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ORDER_TYPE = "float";
    private static final String DIRECTION = "from";
    private static final String FROM_CCY = "BTCLN";
    private static final int REQUIRED_CONFIRMATIONS = 1;

    private final FixedFloatClient client;
    private final FixedFloatProperties properties;
    private final ConversionOrderRepository repository;
    private final ObjectMapper objectMapper;
    private final LightningToOnchainRateRepository lightningToOnchainRateRepository;

    @Override
    public ConversionOrderEntity createOrGetOrder(UUID processId, String chain, String toAddress, long amountSats) {
        Optional<ConversionOrderEntity> existing = repository.findFirstByProcessIdAndPartnerOrderByCreatedAtDesc(
                processId,
                ConversionPartner.FIXEDFLOAT
        );
        if (existing.isPresent()) {
            return existing.get();
        }

        String fromCcy = FROM_CCY;

        String toCcy = lightningToOnchainRateRepository.findLatestByFromAssetAndNetwork(FROM_CCY, chain)
                .map(LightningToOnchainRateEntity::getToAsset)
                .filter(value -> value != null && !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("Missing FixedFloat mapping for network=" + chain));

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
        if (!isSuccess(response)) {
            throw new IllegalStateException("FixedFloat order creation failed: " + response.msg());
        }

        ConversionOrderEntity entity = new ConversionOrderEntity();
        entity.setProcessId(processId);
        entity.setPartner(ConversionPartner.FIXEDFLOAT);
        entity.setPartnerOrderId(asString(nested(response.data(), "id")));
        entity.setPartnerOrderToken(asString(nested(response.data(), "token")));
        entity.setStatus(asString(nested(response.data(), "status")));
        entity.setFromCcy(fromCcy);
        entity.setToCcy(toCcy);
        entity.setAmount(amountBtc.stripTrailingZeros().toPlainString());
        entity.setToAddress(toAddress);
        entity.setConfirmations(asInteger(nested(response.data(), "from", "tx", "confirmations")));
        entity.setPaymentRequest(extractPaymentRequest(response.data()));
        entity.setRequestPayload(payload);
        entity.setResponsePayload(toJson(response));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        if (entity.getPartnerOrderId() == null || entity.getPartnerOrderToken() == null) {
            throw new IllegalStateException("FixedFloat order response missing id/token");
        }

        repository.save(entity);
        return entity;
    }

    @Override
    public ConversionOrderEntity refreshOrder(ConversionOrderEntity entity) {
        FixedFloatOrderRequest request = new FixedFloatOrderRequest(
                entity.getPartnerOrderId(),
                entity.getPartnerOrderToken()
        );
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

    @Override
    public boolean isCompleted(ConversionOrderEntity entity) {
        Integer confirmations = entity.getConfirmations();
        int required = REQUIRED_CONFIRMATIONS;
        if (confirmations == null) {
            return false;
        }
        return confirmations >= required;
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

    private String extractPaymentRequest(Map<String, Object> data) {
        String invoice = asString(nested(data, "invoice"));
        if (invoice != null && !invoice.isBlank()) {
            return invoice;
        }
        invoice = asString(nested(data, "payin"));
        if (invoice != null && !invoice.isBlank()) {
            return invoice;
        }
        invoice = asString(nested(data, "payin", "invoice"));
        if (invoice != null && !invoice.isBlank()) {
            return invoice;
        }
        invoice = asString(nested(data, "payinInvoice"));
        if (invoice != null && !invoice.isBlank()) {
            return invoice;
        }
        invoice = asString(nested(data, "from", "address"));
        if (invoice != null && !invoice.isBlank()) {
            return invoice;
        }
        return asString(nested(data, "from", "addressAlt"));
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
