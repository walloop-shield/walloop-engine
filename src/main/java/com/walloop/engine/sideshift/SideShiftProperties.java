package com.walloop.engine.sideshift;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sideshift")
public class SideShiftProperties {

    /**
     * Base URL da API SideShift.
     */
    private String baseUrl = "https://sideshift.ai/api/v2";

    /**
     * Coin/rede de destino (Liquid USDT).
     */
    private String settleCoin = "usdt";
    private String settleNetwork = "liquid";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSettleCoin() {
        return settleCoin;
    }

    public void setSettleCoin(String settleCoin) {
        this.settleCoin = settleCoin;
    }

    public String getSettleNetwork() {
        return settleNetwork;
    }

    public void setSettleNetwork(String settleNetwork) {
        this.settleNetwork = settleNetwork;
    }
}

