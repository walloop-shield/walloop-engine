package io.walloop.engine.pairs;

import java.math.BigDecimal;

public record PairAvailabilityItem(
        String partner,
        String from,
        String to,
        boolean available,
        BigDecimal min,
        BigDecimal max
) {
}

