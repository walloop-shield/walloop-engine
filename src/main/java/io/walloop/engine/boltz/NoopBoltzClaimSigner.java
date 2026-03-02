package io.walloop.engine.boltz;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(BoltzClaimSigner.class)
@Slf4j
public class NoopBoltzClaimSigner implements BoltzClaimSigner {

    @Override
    public Optional<BoltzClaimSignature> sign(BoltzClaimSigningRequest request) {
        log.warn("NoopBoltzClaimSigner - claim signer not configured swapId={}", request.swapId());
        return Optional.empty();
    }
}

