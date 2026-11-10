package com.fadaiting.camp.service;

public interface LineIdTokenClient {
    String verify(String idToken, String channelId);
}
