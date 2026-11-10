package com.fadaiting.camp;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JwtTest {

    @Test
    public void testGen(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("id",1);
        claims.put("username","Jack");
        //生成jwt代碼
        String token = JWT.create()
                .withClaim("user",claims)//添加載負
                .withExpiresAt(new Date(System.currentTimeMillis()-1000*60*60*12))//添加過期時間
                .sign(Algorithm.HMAC256("jack"));//指定算法，配置密鑰
    }

    @Test
    public void testPass(){
        //定義字符串，用戶傳來的token
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGFpbXMiOnsiaWQiOjMsInVzZXJuYW1lIjoid2FuZ2JhIn0sImV4cCI6MTcwNDcyMjQ0MX0.WCERQ-rmVjfb6B7P-BG8VcvkBWQBLykyOOR5929E1lY";
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256("jack")).build();
        org.junit.jupiter.api.Assertions.assertThrows(com.auth0.jwt.exceptions.TokenExpiredException.class,
                () -> jwtVerifier.verify(token));
//        System.out.println(claims.get("user"));
        //如果竄改頭部或載荷數據，驗證失敗
        //token不一致，驗證失敗
        //token過期，驗證失敗
    }
}
