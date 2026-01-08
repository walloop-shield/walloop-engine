package com.walloop.engine.lightning;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(prefix = "walloop.lightning.lsp", name = {"base-url", "api-key"})
@RequiredArgsConstructor
@Slf4j
public class HttpLspLiquidityService implements LspLiquidityService {

    private static final String OFFER_RECOMMENDATIONS_QUERY =
            "query($channelSize: Float!, $offerType: MarketOfferType) {"
                    + " getOfferRecommendations(channelSize: $channelSize, offerType: $offerType) { id }"
                    + " }";
    private static final String CREATE_ORDER_MUTATION =
            "mutation($offer: String!, $size: Float!, $pubkey: String, $paymentMethod: OrderPaymentMethod) {"
                    + " createOrder(input: { offer: $offer, size: $size, pubkey: $pubkey, payment_method: $paymentMethod })"
                    + " { id orderId }"
                    + " }";

    private static final String LSP_GRAPHQL_PATH = "/graphql";

    private final ObjectMapper objectMapper;

    @Value("${walloop.lightning.lsp.base-url:}")
    private String baseUrl;

    @Value("${walloop.lightning.lsp.api-key:}")
    private String apiKey;

    @Override
    public LspLiquidityResponse requestInboundLiquidity(LspLiquidityRequest request) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("LSP base URL not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LSP API key not configured");
        }

        String endpoint = resolveEndpoint();
        RestClient client = RestClient.builder().build();

        Map<String, Object> offerVariables = new HashMap<>();
        offerVariables.put("channelSize", (double) request.requestedSats());
        offerVariables.put("offerType", "CHANNEL");
        String offerResponse = executeGraphql(client, endpoint, OFFER_RECOMMENDATIONS_QUERY, offerVariables);
        String offerId = extractOfferId(offerResponse);
        if (offerId == null || offerId.isBlank()) {
            throw new IllegalStateException("LSP offer recommendation not found");
        }

        Map<String, Object> orderVariables = new HashMap<>();
        orderVariables.put("offer", offerId);
        orderVariables.put("size", (double) request.requestedSats());
        orderVariables.put("pubkey", request.nodePubKey());
        orderVariables.put("paymentMethod", "USD");
        String orderResponse = executeGraphql(client, endpoint, CREATE_ORDER_MUTATION, orderVariables);

        String externalId = extractOrderId(orderResponse);
        if (externalId == null || externalId.isBlank()) {
            externalId = offerId;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("offerResponse", offerResponse);
        payload.put("orderResponse", orderResponse);

        String responsePayload = serializePayload(payload);
        log.info("LSP order requested processId={} externalId={}", request.processId(), externalId);
        return new LspLiquidityResponse(externalId, responsePayload);
    }

    private String executeGraphql(RestClient client, String endpoint, String query, Map<String, Object> variables) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("variables", variables);
        return client.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    headers.setBearerAuth(apiKey);
                })
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private String extractOfferId(String response) {
        return extractFirstId(response, "getOfferRecommendations");
    }

    private String extractOrderId(String response) {
        return extractFirstId(response, "createOrder");
    }

    private String extractFirstId(String response, String dataKey) {
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
            Object data = payload.get("data");
            if (!(data instanceof Map<?, ?> dataMap)) {
                return null;
            }
            Object node = dataMap.get(dataKey);
            if (node instanceof Map<?, ?> nodeMap) {
                Object id = nodeMap.get("id");
                if (id == null) {
                    id = nodeMap.get("orderId");
                }
                return id == null ? null : id.toString();
            }
            if (node instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item instanceof Map<?, ?> itemMap) {
                        Object id = itemMap.get("id");
                        if (id != null) {
                            return id.toString();
                        }
                    }
                }
            }
            if (node != null && !(node instanceof Boolean)) {
                return node.toString();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveEndpoint() {
        String normalizedBase = baseUrl.trim();
        if (normalizedBase.endsWith(LSP_GRAPHQL_PATH)) {
            return normalizedBase;
        }
        String trimmed = normalizedBase.endsWith("/") ? normalizedBase.substring(0, normalizedBase.length() - 1) : normalizedBase;
        return trimmed + LSP_GRAPHQL_PATH;
    }
}
