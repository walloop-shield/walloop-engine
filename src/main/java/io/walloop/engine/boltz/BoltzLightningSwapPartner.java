package io.walloop.engine.boltz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.walloop.engine.lightning.swap.LightningSwapPartner;
import io.walloop.engine.lightning.swap.LightningSwapRequest;
import io.walloop.engine.lightning.swap.LightningSwapResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoltzLightningSwapPartner implements LightningSwapPartner {

    private final BoltzClient boltzClient;
    private final ObjectMapper objectMapper;

    @Override
    public LightningSwapResult createSwap(LightningSwapRequest request) {
        BoltzSubmarineRequest payload = BoltzSubmarineRequest.builder()
                .from(request.fromAsset())
                .to(request.toAsset())
                .invoice(request.invoice())
                .refundPublicKey(request.refundPublicKey())
                .build();
        BoltzSubmarineResponse response = boltzClient.createSubmarineSwap(payload);
        return new LightningSwapResult(
                response == null ? null : response.id(),
                response == null ? null : response.address(),
                response == null ? null : response.expectedAmount(),
                toJson(payload),
                toJson(response)
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Boltz payload", e);
        }
    }
}

