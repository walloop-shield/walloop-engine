package com.walloop.engine.sideshift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class SideShiftClient {

    private final SideShiftProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SideShiftShiftResponse createShift(
            String depositCoin,
            String depositNetwork,
            String settleAddress
    ) {
        Map<String, Object> payload = Map.of(
                "depositCoin", depositCoin.toLowerCase(),
                "depositNetwork", depositNetwork.toLowerCase(),
                "settleCoin", properties.getSettleCoin(),
                "settleNetwork", properties.getSettleNetwork(),
                "settleAddress", settleAddress
        );

        RequestEntity<Map<String, Object>> request = RequestEntity
                .post(URI.create(properties.getBaseUrl() + "/shift"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("SideShift createShift failed: " + response.getStatusCode());
        }

        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            if (node.hasNonNull("error") && !node.get("error").isNull()) {
                throw new IllegalStateException("SideShift error: " + node.get("error").toString());
            }
            return new SideShiftShiftResponse(
                    node.path("id").asText(),
                    node.path("depositAddress").asText(),
                    node.path("depositCoin").asText(),
                    node.path("depositNetwork").asText(),
                    node.path("settleCoin").asText(),
                    node.path("settleNetwork").asText()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse SideShift response", e);
        }
    }
}

