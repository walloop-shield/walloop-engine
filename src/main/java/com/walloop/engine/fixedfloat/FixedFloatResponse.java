package com.walloop.engine.fixedfloat;

public record FixedFloatResponse<T>(
        String code,
        String msg,
        T data
) {
}
