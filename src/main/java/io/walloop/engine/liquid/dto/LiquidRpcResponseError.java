package io.walloop.engine.liquid.dto;

import lombok.Value;

@Value
public class LiquidRpcResponseError {
    int code;
    String message;
}


