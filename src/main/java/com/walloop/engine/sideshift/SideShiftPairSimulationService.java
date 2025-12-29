package com.walloop.engine.sideshift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walloop.engine.core.DepositWatchEntity;
import com.walloop.engine.core.DepositWatchRepository;
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

    public void ensureSimulation(UUID processId, String depositCoin, String depositNetwork) {
        if (repository.findFirstByProcessIdOrderByCreatedAtDesc(processId).isPresent()) {
            return;
        }

        String fromCoin = depositCoin;
        String fromNetwork = depositNetwork;
        String toCoin = SideShiftSwapService.SETTLE_COIN;
        String toNetwork = SideShiftSwapService.SETTLE_NETWORK;

        DepositWatchEntity watch = depositWatchRepository.findByProcessId(processId)
                .orElseThrow(() -> new IllegalStateException("Deposit watch not found for processId=" + processId));
        String amount = watch.getLastBalance();
        if (amount == null || amount.isBlank()) {
            throw new IllegalStateException("Deposit watch lastBalance not available for processId=" + processId);
        }

        Map<String, Object> response = client.getPair(fromCoin, toCoin, amount, fromNetwork, toNetwork);

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
                "from", fromCoin,
                "to", toCoin,
                "amount", amount,
                "depositNetwork", fromNetwork,
                "settleNetwork", toNetwork
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
