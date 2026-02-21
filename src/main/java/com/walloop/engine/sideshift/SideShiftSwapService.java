package com.walloop.engine.sideshift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.network.NetworkAssetService;
import com.walloop.engine.onboarding.LoginSessionEntity;
import com.walloop.engine.onboarding.LoginSessionRepository;
import com.walloop.engine.swap.SwapOrderEntity;
import com.walloop.engine.swap.SwapOrderRepository;
import com.walloop.engine.swap.SwapOrderStatus;
import com.walloop.engine.swap.SwapPartner;
import com.walloop.engine.swap.SwapStatusScheduler;
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
    private final SwapOrderRepository orderRepository;
    private final SwapStatusScheduler statusScheduler;
    private final ObjectMapper objectMapper;
    private final LoginSessionRepository loginSessionRepository;
    private final NetworkAssetService networkAssetService;

    public SideShiftShiftResponse swapToLiquid(
            String depositCoin,
            String depositNetwork,
            String settleAddress,
            String refundAddress,
            UUID processId,
            UUID ownerId
    ) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SideShift secret is not configured");
        }
        String userIp = resolveUserIp(ownerId);
        String mainAsset = networkAssetService.requireMainAsset(depositCoin);
        SideShiftCreateVariableShiftRequest request = SideShiftCreateVariableShiftRequest.builder()
                .depositCoin(mainAsset.toLowerCase())
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
        SwapOrderEntity entity = new SwapOrderEntity();
        entity.setProcessId(processId);
        entity.setPartner(SwapPartner.SIDESHIFT);
        entity.setPartnerOrderId(response.id());
        entity.setDepositAddress(response.depositAddress());
        entity.setDepositNetwork(response.depositNetwork());
        entity.setUserIp(userIp);
        entity.setStatus(SwapOrderStatus.CREATED);
        entity.setRequestPayload(toJson(request));
        entity.setResponsePayload(toJson(response));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(entity);
        statusScheduler.ensurePolling();
    }

    private String resolveUserIp(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        return loginSessionRepository.findFirstByUserIdOrderByCreatedAtDesc(ownerId)
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
