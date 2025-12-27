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
     * Coin/rede de destino (Liquid BTC).
     */
    private String settleCoin = "btc";
    private String settleNetwork = "liquid";

    /**
     * Account secret da SideShift (header x-sideshift-secret).
     */
    private String secret;

    /**
     * Affiliate ID opcional.
     */
    private String affiliateId;

    /**
     * IP do usuario final (header x-user-ip), quando exigido.
     */
    private String userIp;

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

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getAffiliateId() {
        return affiliateId;
    }

    public void setAffiliateId(String affiliateId) {
        this.affiliateId = affiliateId;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }
}
