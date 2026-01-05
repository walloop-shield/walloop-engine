package com.walloop.engine.network;

import com.walloop.engine.wallet.WalletNetworkClient;
import com.walloop.engine.wallet.dto.NetworkAssetResponse;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkAssetService {

    private final WalletNetworkClient walletNetworkClient;

    public Optional<String> findMainAsset(String network) {
        if (network == null || network.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(network);
        for (NetworkAssetResponse asset : fetchNetworks()) {
            if (asset == null) {
                continue;
            }
            if (matches(normalized, asset)) {
                return Optional.ofNullable(asset.mainAsset());
            }
        }
        return Optional.empty();
    }

    public String requireMainAsset(String network) {
        return findMainAsset(network)
                .orElseThrow(() -> new IllegalArgumentException("Unknown network: " + network));
    }

    private List<NetworkAssetResponse> fetchNetworks() {
        try {
            List<NetworkAssetResponse> assets = walletNetworkClient.listNetworks();
            return assets == null ? List.of() : assets;
        } catch (Exception ex) {
            log.warn("Failed to fetch network catalog from wallet service", ex);
            return List.of();
        }
    }

    private boolean matches(String normalized, NetworkAssetResponse asset) {
        if (normalized.equals(normalize(asset.network()))) {
            return true;
        }
        if (normalized.equals(normalize(asset.mainAsset()))) {
            return true;
        }
        List<String> aliases = asset.aliases();
        if (aliases == null || aliases.isEmpty()) {
            return false;
        }
        for (String alias : aliases) {
            if (normalized.equals(normalize(alias))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace("_", "-");
    }
}
