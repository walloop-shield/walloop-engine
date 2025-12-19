package com.walloop.engine.liquid.dto;

import lombok.Value;

@Value
public class LiquidRpcResponse<T> {
    T result;
    LiquidRpcResponseError error;
}

