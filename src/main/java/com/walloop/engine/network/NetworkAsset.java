package com.walloop.engine.network;

import java.util.Locale;
import java.util.Optional;

public enum NetworkAsset {
    BITCOIN("bitcoin", "BTC", "btc"),
    LIGHTNING("lightning", "BTC", "ln"),
    LIQUID("liquid", "BTC", "lbtc", "liquid-btc"),
    ETHEREUM("ethereum", "ETH", "eth"),
    BNB("bsc", "BNB", "bnb", "binance-smart-chain"),
    MATIC("polygon", "MATIC", "matic"),
    ARB("arbitrum", "ARB", "arb"),
    AVAX("avalanche", "AVAX", "avax"),
    BASE("base", "BASE", "base-chain"),
    OP("optimism", "OP", "op"),
    TRON("tron", "TRX", "trx"),
    DOGE("doge", "DOGE", "dogecoin"),
    BCH("bch", "BCH", "bitcoin-cash"),
    DOT("dot", "DOT", "polkadot"),
    ADA("ada", "ADA", "cardano");

    private final String network;
    private final String mainAsset;
    private final String[] aliases;

    NetworkAsset(String network, String mainAsset, String... aliases) {
        this.network = network;
        this.mainAsset = mainAsset;
        this.aliases = aliases == null ? new String[0] : aliases;
    }

    public String getNetwork() {
        return network;
    }

    public String getMainAsset() {
        return mainAsset;
    }

    public static Optional<NetworkAsset> fromNetwork(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(value);
        for (NetworkAsset asset : values()) {
            if (asset.network.equals(normalized)) {
                return Optional.of(asset);
            }
            if (asset.mainAsset.equalsIgnoreCase(normalized)) {
                return Optional.of(asset);
            }
            for (String alias : asset.aliases) {
                if (alias.equalsIgnoreCase(normalized)) {
                    return Optional.of(asset);
                }
            }
        }
        return Optional.empty();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace("_", "-");
    }
}
