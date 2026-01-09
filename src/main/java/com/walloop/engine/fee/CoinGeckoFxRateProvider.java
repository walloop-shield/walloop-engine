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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "walloop.fee.rate-provider", name = "type", havingValue = "coingecko")
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoFxRateProvider implements FxRateProvider {

    private static final String ENDPOINT = "/api/v3/simple/price";

    private final ObjectMapper objectMapper;

    @Value("${walloop.fee.coingecko.base-url:https://api.coingecko.com}")
    private String baseUrl;

    @Value("${walloop.fee.coingecko.api-key:}")
    private String apiKey;

    @Override
    public Optional<FxRateSnapshot> fetch() {
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();
            String response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(ENDPOINT)
                            .queryParam("ids", "bitcoin")
                            .queryParam("vs_currencies", "usd,brl")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.add("x-cg-pro-api-key", apiKey);
                        }
                    })
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
            Object bitcoin = payload.get("bitcoin");
            if (!(bitcoin instanceof Map<?, ?> btcMap)) {
                return Optional.empty();
            }

            BigDecimal btcUsd = toBigDecimal(btcMap.get("usd"));
            BigDecimal btcBrl = toBigDecimal(btcMap.get("brl"));
            if (btcUsd == null) {
                return Optional.empty();
            }
            BigDecimal usdBrl = btcBrl != null && btcUsd.compareTo(BigDecimal.ZERO) > 0
                    ? btcBrl.divide(btcUsd, 8, RoundingMode.DOWN)
                    : null;

            return Optional.of(new FxRateSnapshot(btcUsd, usdBrl));
        } catch (Exception e) {
            log.warn("Failed to fetch CoinGecko rates", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<BigDecimal> fetchAssetUsd(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();
            String response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(ENDPOINT)
                            .queryParam("ids", assetId)
                            .queryParam("vs_currencies", "usd")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.add("x-cg-pro-api-key", apiKey);
                        }
                    })
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
            Object asset = payload.get(assetId);
            if (!(asset instanceof Map<?, ?> assetMap)) {
                return Optional.empty();
            }
            return Optional.ofNullable(toBigDecimal(assetMap.get("usd")));
        } catch (Exception e) {
            log.warn("Failed to fetch CoinGecko price for asset={}", assetId, e);
            return Optional.empty();
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
