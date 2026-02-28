package io.walloop.engine.sideshift;

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
     * Account secret da SideShift (header x-sideshift-secret).
     */
    private String secret;

    /**
     * Affiliate ID opcional.
     */
    private String affiliateId;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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

}

