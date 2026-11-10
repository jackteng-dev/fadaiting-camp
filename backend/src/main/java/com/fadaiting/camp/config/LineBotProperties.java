package com.fadaiting.camp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "line.bot")
public class LineBotProperties {
    private boolean enabled;
    private String channelSecret;
    private String channelAccessToken;
    private String apiBaseUrl;
    private String frontendBaseUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChannelSecret() {
        return channelSecret;
    }

    public void setChannelSecret(String channelSecret) {
        this.channelSecret = channelSecret;
    }

    public String getChannelAccessToken() {
        return channelAccessToken;
    }

    public void setChannelAccessToken(String channelAccessToken) {
        this.channelAccessToken = channelAccessToken;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }
}
