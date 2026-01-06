package com.walloop.engine.sideshift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.onboarding.LoginSessionEntity;
import com.walloop.engine.onboarding.LoginSessionRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SideShiftSwapService {

    public static final String SETTLE_COIN = "btc";
    public static final String SETTLE_NETWORK = "liquid";

    private final SideShiftClient client;
    private final SideShiftProperties properties;
    private final SideShiftShiftRepository shiftRepository;
    private final SideShiftStatusScheduler statusScheduler;
    private final ObjectMapper objectMapper;
    private final LoginSessionRepository loginSessionRepository;

    public SideShiftShiftResponse swapToLiquid(
            String depositCoin,
            String depositNetwork,
            String settleAddress,
            String refundAddress,
            UUID processId,
            String sessionToken
    ) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SideShift secret is not configured");
        }
        String userIp = resolveUserIp(sessionToken);
        SideShiftCreateVariableShiftRequest request = SideShiftCreateVariableShiftRequest.builder()
                .depositCoin(depositCoin.toLowerCase())
                .depositNetwork(depositNetwork.toLowerCase())
                .settleCoin(SETTLE_COIN)
                .settleNetwork(SETTLE_NETWORK)
                .settleAddress(settleAddress)
                .refundAddress(refundAddress)
                .affiliateId(properties.getAffiliateId())
                .build();
        SideShiftShiftResponse response = client.createVariableShift(secret, userIp, request);
        persistShift(processId, userIp, request, response);
        return response;
    }

    private void persistShift(
            UUID processId,
            String userIp,
            SideShiftCreateVariableShiftRequest request,
            SideShiftShiftResponse response
    ) {
        SideShiftShiftEntity entity = new SideShiftShiftEntity();
        entity.setProcessId(processId);
        entity.setShiftId(response.id());
        entity.setDepositAddress(response.depositAddress());
        entity.setDepositNetwork(response.depositNetwork());
        entity.setUserIp(userIp);
        entity.setStatus(SideShiftShiftStatus.CREATED);
        entity.setRequestPayload(toJson(request));
        entity.setResponsePayload(toJson(response));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        shiftRepository.save(entity);
        statusScheduler.ensurePolling();
    }

    private String resolveUserIp(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return null;
        }
        return loginSessionRepository.findBySessionToken(sessionToken)
                .map(LoginSessionEntity::getIpAddress)
                .filter(ip -> !ip.isBlank())
                .orElse(null);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SideShift payload", e);
        }
    }
}
