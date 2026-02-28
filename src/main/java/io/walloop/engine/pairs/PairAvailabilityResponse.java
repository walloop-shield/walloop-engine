package io.walloop.engine.pairs;

import java.util.List;

public record PairAvailabilityResponse(
        String network,
        List<PairAvailabilityItem> pairs
) {
}

