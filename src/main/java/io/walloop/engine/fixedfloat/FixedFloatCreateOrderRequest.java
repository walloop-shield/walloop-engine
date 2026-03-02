package io.walloop.engine.fixedfloat;

import java.math.BigDecimal;

public record FixedFloatCreateOrderRequest(
        String type,
        String fromCcy,
        String toCcy,
        String direction,
        BigDecimal amount,
        String toAddress,
        String tag,
        String refcode,
        BigDecimal afftax
) {
}

