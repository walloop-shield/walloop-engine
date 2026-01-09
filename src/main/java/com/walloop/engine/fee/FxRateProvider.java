package com.walloop.engine.fee;

import java.util.Optional;

public interface FxRateProvider {
  Optional<FxRateSnapshot> fetch();

  default Optional<java.math.BigDecimal> fetchAssetUsd(String assetId) {
    return Optional.empty();
  }
}
