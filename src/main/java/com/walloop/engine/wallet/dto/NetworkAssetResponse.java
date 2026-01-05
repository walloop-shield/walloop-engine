package com.walloop.engine.wallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkAssetResponse(
        String network,
        String mainAsset,
        List<String> aliases
) {
}
