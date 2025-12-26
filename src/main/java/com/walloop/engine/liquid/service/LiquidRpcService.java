package com.walloop.engine.liquid.service;

import com.walloop.engine.liquid.client.LiquidRpcClient;
import com.walloop.engine.liquid.dto.LiquidRpcRequest;
import com.walloop.engine.liquid.dto.LiquidRpcResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiquidRpcService {

    private final LiquidRpcClient client;

    public String getNewAddress(String label) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("getnewaddress")
                .params(java.util.List.of(label, "bech32"))
                .build();

        LiquidRpcResponse<String> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        return response.getResult();
    }

    public String dumpPrivateKey(String address) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("dumpprivkey")
                .params(java.util.List.of(address))
                .build();

        LiquidRpcResponse<String> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        return response.getResult();
    }
}
