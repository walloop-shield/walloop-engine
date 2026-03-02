package io.walloop.engine.boltz;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpBoltzClaimSigner implements BoltzClaimSigner {

    @Value("${boltz.claim.signer-url:}")
    private String signerUrl;

    @Override
    public Optional<BoltzClaimSignature> sign(BoltzClaimSigningRequest request) {
        if (signerUrl == null || signerUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(signerUrl)
                    .build();
            BoltzClaimSignature response = client.post()
                    .uri("/sign")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BoltzClaimSignature.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("HttpBoltzClaimSigner - sign failed swapId={} error={}",
                    request.swapId(),
                    ex.getMessage());
            return Optional.empty();
        }
    }
}

