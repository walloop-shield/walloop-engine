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
     * Parametros para simulacao do par (USDT -> BTC em Liquid).
     */
    private String simulationFromCoin = "usdt";
    private String simulationFromNetwork = "liquid";
    private String simulationToCoin = "btc";
    private String simulationToNetwork = "liquid";


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

    public String getSimulationFromCoin() {
        return simulationFromCoin;
    }

    public void setSimulationFromCoin(String simulationFromCoin) {
        this.simulationFromCoin = simulationFromCoin;
    }

    public String getSimulationFromNetwork() {
        return simulationFromNetwork;
    }

    public void setSimulationFromNetwork(String simulationFromNetwork) {
        this.simulationFromNetwork = simulationFromNetwork;
    }

    public String getSimulationToCoin() {
        return simulationToCoin;
    }

    public void setSimulationToCoin(String simulationToCoin) {
        this.simulationToCoin = simulationToCoin;
    }

    public String getSimulationToNetwork() {
        return simulationToNetwork;
    }

    public void setSimulationToNetwork(String simulationToNetwork) {
        this.simulationToNetwork = simulationToNetwork;
    }

}
