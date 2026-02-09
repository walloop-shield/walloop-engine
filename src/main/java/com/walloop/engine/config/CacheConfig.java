package com.walloop.engine.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${walloop.pair-availability.cache-seconds:60}") long pairAvailabilitySeconds,
            @Value("${walloop.fee.coincap.cache-seconds:300}") long fxRateSeconds
    ) {
        CaffeineCache pairAvailability = new CaffeineCache(
                "pairAvailability",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(pairAvailabilitySeconds))
                        .maximumSize(200)
                        .build()
        );
        CaffeineCache fxRates = new CaffeineCache(
                "fxRates",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(fxRateSeconds))
                        .maximumSize(200)
                        .build()
        );
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(pairAvailability, fxRates));
        return manager;
    }
}
