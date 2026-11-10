package com.fadaiting.camp.utils;

import com.fadaiting.camp.service.CampAccessService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final CampAccessService access = mock(CampAccessService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(redis, access, new com.fadaiting.camp.service.SessionCookieService(new com.fadaiting.camp.config.AuthCookieProperties()));

    @BeforeEach
    void setup() {
        new JwtUtil("僅供單元測試使用的固定金鑰-123456789012345678901234567890");
        SecurityContextHolder.clearContext();
        when(redis.hasKey("userInfo:1")).thenReturn(true);
        when(access.isCurrentSession(eq(1), any())).thenReturn(true);
        when(access.currentRole(1)).thenReturn("STAFF");
    }

    private int request(String path, String method, String tokenType, boolean cookie) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/fadaiting-camp" + path);
        request.setContextPath("/fadaiting-camp");
        if (cookie) request.setCookies(new Cookie("__Host-camp_access", JwtUtil.genAccessToken(
                Map.of("id", 1, "username", "synthetic", "token_type", tokenType, "authVersion", 0))));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reached = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> reached.set(true));
        assertEquals(response.getStatus() == 200, reached.get());
        return response.getStatus();
    }

    @Test
    void unauthenticatedAndRevokedSessionsAreRejected() throws Exception {
        assertEquals(401, request("/api/booking/list", "POST", "access", false));
        when(redis.hasKey("userInfo:1")).thenReturn(false);
        assertEquals(401, request("/api/booking/list", "POST", "access", true));
    }

    @Test
    void oldSessionCannotResumeAfterAccountIsEnabledAgain() throws Exception {
        when(access.isCurrentSession(eq(1), any())).thenReturn(false);
        assertEquals(401, request("/api/booking/list", "POST", "access", true));
    }

    @Test
    void refreshTokenCannotServeAsAccessToken() throws Exception {
        assertEquals(401, request("/api/booking/list", "POST", "refresh", true));
    }

    @Test
    void directApiAliasesCannotBypassFinancialGate() throws Exception {
        assertEquals(200, request("/api/booking/list", "POST", "access", true));
        assertEquals(403, request("/api/payment/manual", "POST", "access", true));
        assertEquals(403, request("/payment/manual", "POST", "access", true));
        assertEquals(403, request("/api/booking/detail", "PATCH", "access", true));
    }

    @Test
    void databaseRoleRevocationOverridesExistingJwt() throws Exception {
        when(access.currentRole(1)).thenReturn("ADMIN", "STAFF", "");
        assertEquals(200, request("/api/payment/manual", "POST", "access", true));
        assertEquals(403, request("/api/payment/manual", "POST", "access", true));
        assertEquals(403, request("/api/booking/list", "POST", "access", true));
    }

    @Test
    void publicCallbacksReachTheirExistingSignatureValidation() throws Exception {
        assertEquals(200, request("/api/public/payment/ecpay/callback", "POST", "access", false));
        verifyNoInteractions(redis, access);
    }
}
