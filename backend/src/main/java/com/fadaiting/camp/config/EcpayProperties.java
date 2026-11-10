package com.fadaiting.camp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.ecpay")
public class EcpayProperties {
    private boolean enabled;
    private String merchantId;
    private String hashKey;
    private String hashIv;
    private String checkoutUrl;
    private String returnUrl;
    private String orderResultUrl;
    private String resultPageBaseUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }

    public String getHashIv() {
        return hashIv;
    }

    public void setHashIv(String hashIv) {
        this.hashIv = hashIv;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getOrderResultUrl() {
        return orderResultUrl;
    }

    public void setOrderResultUrl(String orderResultUrl) {
        this.orderResultUrl = orderResultUrl;
    }

    public String getResultPageBaseUrl() {
        return resultPageBaseUrl;
    }

    public void setResultPageBaseUrl(String resultPageBaseUrl) {
        this.resultPageBaseUrl = resultPageBaseUrl;
    }
}
