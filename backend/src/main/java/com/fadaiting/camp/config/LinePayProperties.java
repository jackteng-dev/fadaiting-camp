package com.fadaiting.camp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.line-pay")
public class LinePayProperties {
    private boolean enabled;
    private String channelId;
    private String channelSecret;
    private String apiBaseUrl;
    private String confirmUrlBase;
    private String cancelUrl;
    private String deviceProfileId;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelSecret() {
        return channelSecret;
    }

    public void setChannelSecret(String channelSecret) {
        this.channelSecret = channelSecret;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getConfirmUrlBase() {
        return confirmUrlBase;
    }

    public void setConfirmUrlBase(String confirmUrlBase) {
        this.confirmUrlBase = confirmUrlBase;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getDeviceProfileId() {
        return deviceProfileId;
    }

    public void setDeviceProfileId(String deviceProfileId) {
        this.deviceProfileId = deviceProfileId;
    }
}
