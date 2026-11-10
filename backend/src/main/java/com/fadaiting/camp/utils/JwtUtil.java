package com.fadaiting.camp.utils;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private static String KEY;

    public JwtUtil(@Value("${JWT_SECRET:}") String key) {
        if (key == null || key.length() < 32) {
            throw new IllegalArgumentException("請設定至少 32 字元的 JWT_SECRET；不得使用預設共用金鑰");
        }
        KEY = key;
    }

    //創建(刷新)JWT-token
    public static String genRefreshToken(Map<String,Object> claims){
        return JWT.create()
                .withClaim("claims",claims)
                .withExpiresAt(new Date(System.currentTimeMillis()+1000L*60*60*24*7))// 7 天
                .sign(Algorithm.HMAC256(KEY));
    }
    //創建(訪問)JWT-token
    public static String genAccessToken(Map<String,Object> claims){
        return JWT.create()
                .withClaim("claims",claims)
                .withExpiresAt(new Date(System.currentTimeMillis()+1000L*60*15))// 15 分鐘
                .sign(Algorithm.HMAC256(KEY));
    }

    //解析JWT-token
    public static Map<String,Object> parseToken(String token){
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }

    //從cookie中取回jwt令牌
    public static String extractCookieToken(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    //驗證jwt令牌
    public static boolean validate(String token) {
        try {
            JWT.require(Algorithm.HMAC256(KEY))
                    .build()
                    .verify(token); // 驗證簽名 + 過期時間
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
