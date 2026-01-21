package com.walloop.engine.liquid.service;

import com.walloop.engine.liquid.client.LiquidRpcClient;
import com.walloop.engine.liquid.dto.LiquidRpcRequest;
import com.walloop.engine.liquid.dto.LiquidRpcResponse;
import java.math.BigDecimal;
import java.util.Map;
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

        LiquidRpcResponse<Object> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        Object result = response.getResult();
        if (result == null) {
            throw new IllegalStateException("Liquid RPC result missing for getnewaddress");
        }
        return result.toString();
    }

    public String dumpPrivateKey(String address) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("dumpprivkey")
                .params(java.util.List.of(address))
                .build();

        LiquidRpcResponse<Object> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        Object result = response.getResult();
        if (result == null) {
            throw new IllegalStateException("Liquid RPC result missing for dumpprivkey");
        }
        return result.toString();
    }

    public void importPrivateKey(String privateKey, String label, boolean rescan) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("importprivkey")
                .params(java.util.List.of(privateKey, label, rescan))
                .build();

        LiquidRpcResponse<Object> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
    }

    public String sendToAddress(String address, String amount) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("sendtoaddress")
                .params(java.util.List.of(address, amount))
                .build();

        LiquidRpcResponse<Object> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        Object result = response.getResult();
        if (result == null) {
            throw new IllegalStateException("Liquid RPC result missing for sendtoaddress");
        }
        return result.toString();
    }

    public BigDecimal getReceivedByAddress(String address) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("getreceivedbyaddress")
                .params(java.util.List.of(address))
                .build();

        LiquidRpcResponse<Object> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        Object result = response.getResult();
        if (result == null) {
            throw new IllegalStateException("Liquid RPC balance not available for address=" + address);
        }
        if (result instanceof Map<?, ?> mapResult) {
            Object value = resolveAssetBalance(mapResult);
            if (value == null) {
                throw new IllegalStateException("Liquid RPC balance missing bitcoin asset for address=" + address);
            }
            return new BigDecimal(value.toString());
        }
        return new BigDecimal(result.toString());
    }

    private Object resolveAssetBalance(Map<?, ?> mapResult) {
        if (mapResult.containsKey("bitcoin")) {
            return mapResult.get("bitcoin");
        }
        if (mapResult.containsKey("lbtc")) {
            return mapResult.get("lbtc");
        }
        if (mapResult.containsKey("l-btc")) {
            return mapResult.get("l-btc");
        }
        return null;
    }

    public Object decodeRawTransaction(String hex) {
        LiquidRpcRequest request = LiquidRpcRequest.builder()
                .method("decoderawtransaction")
                .params(java.util.List.of(hex))
                .build();

        LiquidRpcResponse<Object> response = client.call(request);
        if (response.getError() != null) {
            throw new IllegalStateException("Liquid RPC error: " + response.getError().getMessage());
        }
        Object result = response.getResult();
        if (result == null) {
            throw new IllegalStateException("Liquid RPC result missing for decoderawtransaction");
        }
        return result;
    }
}
