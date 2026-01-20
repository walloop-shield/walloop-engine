package com.walloop.engine.sideshift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.core.DepositWatchEntity;
import com.walloop.engine.core.DepositWatchRepository;
import com.walloop.engine.network.NetworkAssetService;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SideShiftPairSimulationService {

    private final SideShiftClient client;
    private final SideShiftProperties properties;
    private final SideShiftPairSimulationRepository repository;
    private final ObjectMapper objectMapper;
    private final DepositWatchRepository depositWatchRepository;
    private final NetworkAssetService networkAssetService;

    public void ensureSimulation(UUID processId, String network, String depositNetwork) {
        if (repository.findFirstByProcessIdOrderByCreatedAtDesc(processId).isPresent()) {
            return;
        }

        String fromCoin = networkAssetService.requireMainAsset(network).toLowerCase();
        String fromNetwork = depositNetwork.toLowerCase();
        String toCoin = SideShiftSwapService.SETTLE_COIN;
        String toNetwork = SideShiftSwapService.SETTLE_NETWORK;

        DepositWatchEntity watch = depositWatchRepository.findByProcessId(processId)
                .orElseThrow(() -> new IllegalStateException("Deposit watch not found"));
        String amount = watch.getLastBalance();
        if (amount == null || amount.isBlank()) {
            throw new IllegalStateException("Deposit watch lastBalance not available");
        }

        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SideShift secret is not configured");
        }
        String affiliateId = properties.getAffiliateId();
        if (affiliateId == null || affiliateId.isBlank()) {
            throw new IllegalStateException("SideShift affiliateId is not configured");
        }

        Map<String, Object> response = client.getPair(secret, affiliateId, fromCoin, toCoin);

        SideShiftPairSimulationEntity entity = new SideShiftPairSimulationEntity();
        entity.setProcessId(processId);
        entity.setFromCoin(fromCoin);
        entity.setFromNetwork(fromNetwork);
        entity.setToCoin(toCoin);
        entity.setToNetwork(toNetwork);
        entity.setAmount(amount);
        entity.setLastBalance(amount);
        entity.setMin(asString(response.get("min")));
        entity.setMax(asString(response.get("max")));
        entity.setRate(asString(response.get("rate")));
        entity.setDepositCoin(asString(response.get("depositCoin")));
        entity.setSettleCoin(asString(response.get("settleCoin")));
        entity.setDepositNetwork(asString(response.get("depositNetwork")));
        entity.setSettleNetwork(asString(response.get("settleNetwork")));
        entity.setRequestPayload(toJson(Map.of(
                "affiliateId", affiliateId,
                "from", fromCoin,
                "to", toCoin
        )));
        entity.setCreatedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SideShift pair payload", e);
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
