package com.walloop.engine.lightning;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.LightningAddress;
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
                    + " getOfferRecommendations(channelSize: $channelSize, offerType: $offerType) {"
                    + " list { id min_size max_size status offer_type account min_block_length fee_rate base_fee }"
                    + " }"
                    + " }";
    private static final String CREATE_ORDER_MUTATION =
            "mutation($offer: String!, $size: Float!, $pubkey: String, $paymentMethod: OrderPaymentMethod) {"
                    + " createOrder(input: { offer: $offer, size: $size, pubkey: $pubkey, payment_method: $paymentMethod })"
                    + " }";

    private static final String LSP_GRAPHQL_PATH = "/graphql";

    private final ObjectMapper objectMapper;
    private final SynchronousLndAPI lndApi;

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
        OfferSelection offerSelection = selectOffer(offerResponse, request.requestedSats());
        if (offerSelection == null || offerSelection.id() == null || offerSelection.id().isBlank()) {
            throw new IllegalStateException("LSP offer recommendation not found");
        }
        String nodeAddress = connectToOfferNode(client, endpoint, offerSelection);
        if (nodeAddress == null || nodeAddress.isBlank()) {
            throw new IllegalStateException("Failed to connect to LSP offer node");
        }

        Map<String, Object> orderVariables = new HashMap<>();
        orderVariables.put("offer", offerSelection.id());
        orderVariables.put("size", (double) request.requestedSats());
        orderVariables.put("pubkey", request.nodePubKey());
        orderVariables.put("paymentMethod", "AMBUCKS");
        String orderResponse = executeGraphql(client, endpoint, CREATE_ORDER_MUTATION, orderVariables);

        String externalId = extractOrderId(orderResponse);
        if (externalId == null || externalId.isBlank()) {
            externalId = offerSelection.id();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("offerResponse", offerResponse);
        payload.put("orderResponse", orderResponse);

        String responsePayload = serializePayload(payload);
        log.info("HttpLspLiquidityService - LSP order requested processId={} externalId={}", request.processId(), externalId);
        return new LspLiquidityResponse(externalId, responsePayload, nodeAddress);
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

    private OfferSelection selectOffer(String response, long requestedSats) {
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
            Object data = payload.get("data");
            if (!(data instanceof Map<?, ?> dataMap)) {
                return null;
            }
            Object node = dataMap.get("getOfferRecommendations");
            if (!(node instanceof Map<?, ?> nodeMap)) {
                return null;
            }
            Object offers = nodeMap.get("list");
            if (!(offers instanceof Iterable<?> offerList)) {
                return null;
            }
            OfferSelection best = null;
            for (Object item : offerList) {
                if (item instanceof Map<?, ?> itemMap) {
                    Object id = itemMap.get("id");
                    if (id == null) {
                        continue;
                    }
                    Object status = itemMap.get("status");
                    if (status == null || !"ENABLED".equalsIgnoreCase(status.toString())) {
                        continue;
                    }
                    Long minSize = parseLong(itemMap.get("min_size"));
                    Long maxSize = parseLong(itemMap.get("max_size"));
                    if (minSize != null && requestedSats < minSize) {
                        continue;
                    }
                    if (maxSize != null && requestedSats > maxSize) {
                        continue;
                    }
                    OfferSelection candidate = new OfferSelection(
                            id.toString(),
                            asString(itemMap.get("account")),
                            parseLong(itemMap.get("min_block_length")),
                            parseDouble(itemMap.get("fee_rate")),
                            parseDouble(itemMap.get("base_fee"))
                    );
                    if (best == null) {
                        best = candidate;
                        continue;
                    }
                    best = selectPreferredOffer(best, candidate);
                }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (text.isBlank()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(text.trim()).longValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String connectToOfferNode(RestClient client, String endpoint, OfferSelection offerSelection) {
        String account = offerSelection.account();
        if (account == null || account.isBlank()) {
            log.warn("HttpLspLiquidityService - LSP offer missing account for connect. offerId={}", offerSelection.id());
            return null;
        }

        String pubkey = account;
        if (account.contains("@")) {
            pubkey = account.split("@", 2)[0];
        }

        String host = resolveNodeHost(client, endpoint, pubkey);

        if (host == null || host.isBlank()) {
            log.warn("HttpLspLiquidityService - Unable to resolve LSP offer node address. offerId={} pubkey={}", offerSelection.id(), pubkey);
            return null;
        }

        LightningAddress address = new LightningAddress();
        address.setPubkey(pubkey);
        address.setHost(host);
        try {
            lndApi.connectPeer(address, false, null);
            String nodeAddress = pubkey + "@" + host;
            log.info("HttpLspLiquidityService - Connected to LSP offer node. offerId={} address={}", offerSelection.id(), nodeAddress);
            return nodeAddress;
        } catch (StatusException | ValidationException e) {
            log.warn("HttpLspLiquidityService - Failed to connect to LSP offer node. offerId={} address={}", offerSelection.id(), pubkey + "@" + host, e);
            return null;
        }
    }

    private String resolveNodeHost(RestClient client, String endpoint, String pubkey) {
        String response = executeGraphqlSafely(client, endpoint, nodeQuery(), Map.of("pubkey", pubkey));
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
            Object data = payload.get("data");
            if (!(data instanceof Map<?, ?> dataMap)) {
                return null;
            }
            Object node = dataMap.get("getNode");
            if (!(node instanceof Map<?, ?> nodeMap)) {
                return null;
            }
            Object graphInfo = nodeMap.get("graph_info");
            if (!(graphInfo instanceof Map<?, ?> graphInfoMap)) {
                return null;
            }
            Object graphNode = graphInfoMap.get("node");
            if (!(graphNode instanceof Map<?, ?> graphNodeMap)) {
                return null;
            }
            Object addresses = graphNodeMap.get("addresses");
            if (addresses instanceof Iterable<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> addrMap) {
                        String addr = asString(addrMap.get("addr"));
                        if (addr == null || addr.isBlank()) {
                            addr = asString(addrMap.get("address"));
                        }
                        if (addr != null && addr.contains(":")) {
                            return addr;
                        }
                    } else if (item instanceof String addr && addr.contains(":")) {
                        return addr;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (text.isBlank()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(text.trim()).doubleValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private OfferSelection selectPreferredOffer(OfferSelection current, OfferSelection candidate) {
        long currentMinBlocks = current.minBlockLength() == null ? 0 : current.minBlockLength();
        long candidateMinBlocks = candidate.minBlockLength() == null ? 0 : candidate.minBlockLength();
        if (candidateMinBlocks > currentMinBlocks) {
            return candidate;
        }
        if (candidateMinBlocks < currentMinBlocks) {
            return current;
        }

        double currentFeeRate = current.feeRate() == null ? Double.MAX_VALUE : current.feeRate();
        double candidateFeeRate = candidate.feeRate() == null ? Double.MAX_VALUE : candidate.feeRate();
        if (candidateFeeRate < currentFeeRate) {
            return candidate;
        }
        if (candidateFeeRate > currentFeeRate) {
            return current;
        }

        double currentBaseFee = current.baseFee() == null ? Double.MAX_VALUE : current.baseFee();
        double candidateBaseFee = candidate.baseFee() == null ? Double.MAX_VALUE : candidate.baseFee();
        if (candidateBaseFee < currentBaseFee) {
            return candidate;
        }
        return current;
    }

    private String executeGraphqlSafely(RestClient client, String endpoint, String query, Map<String, Object> variables) {
        try {
            return executeGraphql(client, endpoint, query, variables);
        } catch (Exception e) {
            log.warn("HttpLspLiquidityService - Failed to execute LSP GraphQL query for node lookup", e);
            return null;
        }
    }

    private String nodeQuery() {
        return "query($pubkey: String!) {"
                + " getNode(pubkey: $pubkey) {"
                + " graph_info { node { addresses { addr network } } }"
                + " }"
                + " }";
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
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
                if (id != null) {
                    return id.toString();
                }
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

    private record OfferSelection(
            String id,
            String account,
            Long minBlockLength,
            Double feeRate,
            Double baseFee
    ) {
    }
}
