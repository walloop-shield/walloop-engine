package com.walloop.engine.liquid.client;

import com.walloop.engine.liquid.dto.LiquidRpcRequest;
import com.walloop.engine.liquid.dto.LiquidRpcResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "liquidRpcClient",
        url = "${liquid.rpc.url}",
        configuration = LiquidRpcFeignConfig.class
)
public interface LiquidRpcClient {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    LiquidRpcResponse<Object> call(@RequestBody LiquidRpcRequest request);
}
