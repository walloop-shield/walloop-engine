package com.walloop.engine.fixedfloat;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fixedfloat")
public class FixedFloatProperties {

    private String baseUrl = "https://ff.io/api/v2";
    private String apiKey;
    private String apiSecret;
    private String refcode;
    private String afftax;
    private Map<String, String> toCcyOverrides = new HashMap<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }


    public String getRefcode() {
        return refcode;
    }

    public void setRefcode(String refcode) {
        this.refcode = refcode;
    }

    public String getAfftax() {
        return afftax;
    }

    public void setAfftax(String afftax) {
        this.afftax = afftax;
    }

    public Map<String, String> getToCcyOverrides() {
        return toCcyOverrides;
    }

    public void setToCcyOverrides(Map<String, String> toCcyOverrides) {
        this.toCcyOverrides = toCcyOverrides;
    }
}
