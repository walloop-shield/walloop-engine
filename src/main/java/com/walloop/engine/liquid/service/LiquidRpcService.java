package com.walloop.engine.liquid.service;

import com.walloop.engine.liquid.client.LiquidRpcClient;
import com.walloop.engine.liquid.dto.LiquidRpcRequest;
import com.walloop.engine.liquid.dto.LiquidRpcResponse;
import java.math.BigDecimal;
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

    public void importPrivateKey(String privateKey, String label, boolean rescan) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("importprivkey")
                .params(java.util.List.of(privateKey, label, rescan))
                .build();

        LiquidRpcResponse<String> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
    }

    public String sendToAddress(String address, String amount) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("sendtoaddress")
                .params(java.util.List.of(address, amount))
                .build();

        LiquidRpcResponse<String> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        return response.getResult();
    }

    public BigDecimal getReceivedByAddress(String address) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("getreceivedbyaddress")
                .params(java.util.List.of(address))
                .build();

        LiquidRpcResponse<String> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        if (response.getResult() == null || response.getResult().isBlank()) {
            throw new IllegalStateException("Liquid RPC balance not available for address=" + address);
        }
        return new BigDecimal(response.getResult());
    }
}
