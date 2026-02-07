package com.walloop.engine.pairs;

public record PairAvailabilityItem(
        String partner,
        String from,
        String to,
        boolean available
) {
}
