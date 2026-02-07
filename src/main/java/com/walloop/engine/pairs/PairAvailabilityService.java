package com.walloop.engine.pairs;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PairAvailabilityService {
    private final List<PairAvailabilityProvider> providers;

    public PairAvailabilityResponse checkByNetwork(String network) {
        String normalized = normalize(network);
        if (normalized.isEmpty()) {
            return new PairAvailabilityResponse(network, Collections.emptyList());
        }

        return cachedCheck(normalized, network);
    }

    @Cacheable(cacheNames = "pairAvailability", key = "#normalized")
    public PairAvailabilityResponse cachedCheck(String normalized, String originalNetwork) {
        List<PairAvailabilityItem> results = providers.stream()
                .map(provider -> provider.checkAvailability(normalized))
                .flatMap(Optional::stream)
                .toList();
        return new PairAvailabilityResponse(originalNetwork, results);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
