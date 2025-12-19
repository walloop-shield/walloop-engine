package com.walloop.engine.liquid.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.liquid.config.LiquidRpcProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiquidRpcClient {

    private final LiquidRpcProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public String getNewAddress(String label) {
        Map<String, Object> payload = Map.of(
                "jsonrpc", "1.0",
                "id", "walloop-engine",
                "method", "getnewaddress",
                "params", List.of(label, "bech32")
        );

        RequestEntity<Map<String, Object>> request = RequestEntity
                .post(buildUri())
                .headers(authHeaders())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Liquid RPC getnewaddress failed: " + response.getStatusCode());
        }

        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            if (node.hasNonNull("error") && !node.get("error").isNull()) {
                throw new IllegalStateException("Liquid RPC error: " + node.get("error").toString());
            }
            return node.get("result").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Liquid RPC response", e);
        }
    }

    private URI buildUri() {
        return URI.create("http://" + properties.getHost() + ":" + properties.getPort());
    }

    private HttpHeaders authHeaders() {
        String auth = properties.getUsername() + ":" + properties.getPassword();
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        return headers;
    }
}
