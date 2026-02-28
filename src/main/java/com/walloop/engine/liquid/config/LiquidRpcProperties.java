package com.walloop.engine.liquid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "liquid.rpc")
public class LiquidRpcProperties {

    /**
     * Base URL of the Liquid node (e.g. http://localhost:7041).
     */
    private String url = "http://localhost:7041";

    private String username = "rpcuser";
    private String password = "rpcpassword";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

