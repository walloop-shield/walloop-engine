package io.walloop.engine.pairs;

import java.util.Optional;

public interface PairAvailabilityProvider {

    String partner();

    Optional<PairAvailabilityItem> checkAvailability(String network);
}

