package com.walloop.engine.wallet;

import com.walloop.engine.wallet.dto.NetworkAssetResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "walletNetworkClient", url = "${walloop.wallet.base-url}")
public interface WalletNetworkClient {

    @GetMapping("/v1/chains")
    List<NetworkAssetResponse> listNetworks();
}
