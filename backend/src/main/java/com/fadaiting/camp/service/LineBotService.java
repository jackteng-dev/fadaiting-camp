package com.fadaiting.camp.service;

public interface LineBotService {
    void handleWebhook(String signature, String requestBody);
}
