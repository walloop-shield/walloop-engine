package com.walloop.engine.fixedfloat;

import java.math.BigDecimal;

public record FixedFloatPriceRequest(
        String type,
        String fromCcy,
        String toCcy,
        String direction,
        BigDecimal amount
) {
}
