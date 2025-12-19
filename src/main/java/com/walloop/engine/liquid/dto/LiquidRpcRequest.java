package com.walloop.engine.liquid.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LiquidRpcRequest {
    @Builder.Default
    String jsonrpc = "1.0";
    @Builder.Default
    String id = "walloop-engine";
    String method;
    List<Object> params;
}

