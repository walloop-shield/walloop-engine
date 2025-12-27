package com.walloop.engine.sideshift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SideShiftSwapService {

    private final SideShiftClient client;
    private final SideShiftProperties properties;
    private final SideShiftShiftRepository shiftRepository;
    private final ObjectMapper objectMapper;

    public SideShiftShiftResponse swapToLiquidUsdt(
            String depositCoin,
            String depositNetwork,
            String settleAddress,
            String refundAddress,
            UUID processId
    ) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SideShift secret is not configured");
        }
//TODO - pegar o IP do usuario na tabela LoginSession do schema onboarding
        SideShiftCreateVariableShiftRequest request = SideShiftCreateVariableShiftRequest.builder()
                .depositCoin(depositCoin.toLowerCase())
                .depositNetwork(depositNetwork.toLowerCase())
                .settleCoin(properties.getSettleCoin())
                .settleNetwork(properties.getSettleNetwork())
                .settleAddress(settleAddress)
                .refundAddress(refundAddress)
                .affiliateId(properties.getAffiliateId())
                .build();
        SideShiftShiftResponse response = client.createVariableShift(secret, properties.getUserIp(), request);
        persistShift(processId, request, response);
        return response;
    }

    private void persistShift(
            UUID processId,
            SideShiftCreateVariableShiftRequest request,
            SideShiftShiftResponse response
    ) {
        SideShiftShiftEntity entity = new SideShiftShiftEntity();
        entity.setProcessId(processId);
        entity.setShiftId(response.id());
        entity.setDepositAddress(response.depositAddress());
        entity.setDepositNetwork(response.depositNetwork());
        entity.setStatus(SideShiftShiftStatus.CREATED);
        entity.setRequestPayload(toJson(request));
        entity.setResponsePayload(toJson(response));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        shiftRepository.save(entity);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SideShift payload", e);
        }
    }
}
