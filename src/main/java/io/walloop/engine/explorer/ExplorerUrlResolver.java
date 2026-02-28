package io.walloop.engine.explorer;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExplorerUrlResolver {

    private final ExplorerProperties properties;

    public String buildTxUrl(String chain, String txHash) {
        if (txHash == null || txHash.isBlank()) {
            return null;
        }
        String baseUrl = resolveBaseUrl(chain);
        if (baseUrl == null || baseUrl.isBlank()) {
            return txHash;
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalized + "/tx/" + txHash;
    }

    private String resolveBaseUrl(String chain) {
        if (chain == null || chain.isBlank()) {
            return null;
        }
        String normalized = normalize(chain);
        String direct = properties.getTxBaseUrls().get(normalized);
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        return properties.getTxBaseUrls().get(chain);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace("_", "-");
    }
}

