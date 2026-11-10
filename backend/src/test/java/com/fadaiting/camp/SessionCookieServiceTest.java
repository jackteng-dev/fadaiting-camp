package com.fadaiting.camp;

import com.fadaiting.camp.config.AuthCookieProperties;
import com.fadaiting.camp.service.SessionCookieService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class SessionCookieServiceTest {
    @Test void defaultsToSecureHttpOnlyHostCookie() {
        var response=new MockHttpServletResponse(); new SessionCookieService(new AuthCookieProperties()).write(response,SessionCookieService.ACCESS,"synthetic",900);
        String header=response.getHeader("Set-Cookie"); assertTrue(header.contains("Secure") && header.contains("HttpOnly") && header.contains("SameSite=Lax") && header.contains("Path=/") && header.contains("Max-Age=900")); assertFalse(header.contains("Domain=")); assertTrue(header.startsWith("__Host-camp_access="));
    }
    @Test void localHttpNeedsExplicitOverride() {
        var properties=new AuthCookieProperties(); properties.setSecure(false); var response=new MockHttpServletResponse(); new SessionCookieService(properties).write(response,SessionCookieService.REFRESH,"synthetic",-1);
        String header=response.getHeader("Set-Cookie"); assertFalse(header.contains("Secure") || header.contains("Max-Age=")); assertTrue(header.contains("HttpOnly") && header.contains("SameSite=Lax"));
    }
    @Test void logoutClearsBothCurrentAndLegacyCookieNamesWithSameFlags() {
        var response=new MockHttpServletResponse(); new SessionCookieService(new AuthCookieProperties()).clear(response);
        assertEquals(6,response.getHeaders("Set-Cookie").size()); for(String header:response.getHeaders("Set-Cookie")) assertTrue(header.contains("Max-Age=0") && header.contains("Secure") && header.contains("HttpOnly") && header.contains("SameSite=Lax") && header.contains("Path=/"));
    }
}
