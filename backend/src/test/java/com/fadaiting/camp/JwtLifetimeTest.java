package com.fadaiting.camp;

import com.auth0.jwt.JWT;
import com.fadaiting.camp.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JwtLifetimeTest {
    @Test void accessSignatureExpiresAfterFifteenMinutes() {
        new JwtUtil("僅供單元測試使用的固定金鑰-123456789012345678901234567890");
        long remaining=JWT.decode(JwtUtil.genAccessToken(Map.of("token_type","access"))).getExpiresAtAsInstant().getEpochSecond()-Instant.now().getEpochSecond(); assertTrue(remaining>=899 && remaining<=900);
    }
    @Test void refreshSignatureExpiresAfterSevenDays() {
        new JwtUtil("僅供單元測試使用的固定金鑰-123456789012345678901234567890");
        long remaining=JWT.decode(JwtUtil.genRefreshToken(Map.of("token_type","refresh"))).getExpiresAtAsInstant().getEpochSecond()-Instant.now().getEpochSecond(); assertTrue(remaining>=604799 && remaining<=604800);
    }
}
