package com.walloop.engine.network;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NetworkAssetService {

    public Optional<String> findMainAsset(String network) {
        return NetworkAsset.fromNetwork(network).map(NetworkAsset::getMainAsset);
    }

    public String requireMainAsset(String network) {
        return findMainAsset(network)
                .orElseThrow(() -> new IllegalArgumentException("Unknown network: " + network));
    }
}
