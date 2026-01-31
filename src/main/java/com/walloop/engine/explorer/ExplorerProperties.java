package com.walloop.engine.explorer;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "walloop.explorer")
public class ExplorerProperties {

    private Map<String, String> txBaseUrls = new HashMap<>();

    public Map<String, String> getTxBaseUrls() {
        return txBaseUrls;
    }

    public void setTxBaseUrls(Map<String, String> txBaseUrls) {
        this.txBaseUrls = txBaseUrls;
    }
}
