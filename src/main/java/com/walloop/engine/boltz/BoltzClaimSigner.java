package com.walloop.engine.boltz;

import java.util.Optional;

public interface BoltzClaimSigner {

    Optional<BoltzClaimSignature> sign(BoltzClaimSigningRequest request);
}
