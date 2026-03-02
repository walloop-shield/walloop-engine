package io.walloop.engine.network;

import io.walloop.engine.wallet.WalletNetworkClient;
import io.walloop.engine.wallet.dto.NetworkAssetResponse;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkAssetService {

    private final WalletNetworkClient walletNetworkClient;

    public Optional<NetworkAssetResponse> findAsset(String network) {
        if (network == null || network.isBlank()) {
            return Optional.empty();
        }
        for (NetworkAssetResponse asset : fetchNetworks()) {
            if (asset == null) {
                continue;
            }
            if (network.equalsIgnoreCase(asset.network())) {
                return Optional.of(asset);
            }
        }
        return Optional.empty();
    }

    public String requireMainAsset(String network) {
        return findAsset(network)
                .map(NetworkAssetResponse::mainAsset)
                .orElseThrow(() -> new IllegalArgumentException("Unknown network: " + network));
    }

    private List<NetworkAssetResponse> fetchNetworks() {
        try {
            return walletNetworkClient.listNetworks();
        } catch (Exception ex) {
            log.error("NetworkAssetService - Failed to fetch network catalog from wallet service", ex);
            return List.of();
        }
    }

}

