package com.walloop.engine.fee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "walloop.fee.rate-provider", name = "type", havingValue = "coincap")
@RequiredArgsConstructor
@Slf4j
public class CoinCapFxRateProvider implements FxRateProvider {

    private static final String ASSET_ENDPOINT = "/assets/{id}";
    private static final String RATE_ENDPOINT = "/rates/{id}";

    private final ObjectMapper objectMapper;

    @Value("${walloop.fee.coincap.base-url}")
    private String baseUrl;

    @Value("${walloop.fee.coincap.api-key}")
    private String apiKey;

    @Value("${walloop.fee.coincap.usd-brl-rate-id:brazilian-real}")
    private String usdBrlRateId;

    @Override
    @Cacheable(cacheNames = "fxRates", key = "'snapshot'")
    public Optional<FxRateSnapshot> fetch() {
        try {
            Optional<BigDecimal> btcUsd = fetchAssetUsd("bitcoin");
            if (btcUsd.isEmpty()) {
                return Optional.empty();
            }
            BigDecimal usdBrl = fetchUsdBrlRate();
            return Optional.of(new FxRateSnapshot(btcUsd.get(), usdBrl));
        } catch (Exception e) {
            log.warn("CoinCapFxRateProvider - Failed to fetch CoinCap rates", e);
            return Optional.empty();
        }
    }

    @Override
    @Cacheable(cacheNames = "fxRates", key = "#assetId")
    public Optional<BigDecimal> fetchAssetUsd(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();
            String response = client.get()
                    .uri(ASSET_ENDPOINT, assetId)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.add("Authorization", "Bearer " + apiKey);
                        }
                    })
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return Optional.empty();
            }
            Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
            Object data = payload.get("data");
            if (!(data instanceof Map<?, ?> dataMap)) {
                return Optional.empty();
            }
            BigDecimal priceUsd = toBigDecimal(dataMap.get("priceUsd"));
            return Optional.ofNullable(priceUsd);
        } catch (Exception e) {
            log.warn("CoinCapFxRateProvider - Failed to fetch CoinCap price for asset={}", assetId, e);
            return Optional.empty();
        }
    }

    private BigDecimal fetchUsdBrlRate() {
        if (usdBrlRateId == null || usdBrlRateId.isBlank()) {
            return null;
        }
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();
            String response = client.get()
                    .uri(RATE_ENDPOINT, usdBrlRateId)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.add("Authorization", "Bearer " + apiKey);
                        }
                    })
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return null;
            }
            Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
            Object data = payload.get("data");
            if (!(data instanceof Map<?, ?> dataMap)) {
                return null;
            }
            BigDecimal rateUsd = toBigDecimal(dataMap.get("rateUsd"));
            if (rateUsd == null || rateUsd.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return BigDecimal.ONE.divide(rateUsd, 8, RoundingMode.DOWN);
        } catch (Exception e) {
            log.warn("CoinCapFxRateProvider - Failed to fetch CoinCap USD/BRL rate", e);
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
