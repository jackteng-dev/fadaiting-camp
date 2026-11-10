package com.fadaiting.camp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {
    private boolean secure = true;
    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }
}
