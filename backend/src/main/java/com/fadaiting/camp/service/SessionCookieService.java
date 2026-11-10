package com.fadaiting.camp.service;

import com.fadaiting.camp.config.AuthCookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import com.fadaiting.camp.utils.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class SessionCookieService {
    public static final String ACCESS = "camp_access";
    public static final String REFRESH = "camp_refresh";
    private final AuthCookieProperties properties;

    public SessionCookieService(AuthCookieProperties properties) { this.properties = properties; }

    public void write(HttpServletResponse response, String name, String value, int maxAge) {
        writeNamed(response,cookieName(name),value,maxAge);
    }

    public String cookieName(String name) { return properties.isSecure() ? "__Host-"+name : name; }
    public String extract(HttpServletRequest request, String name) { return JwtUtil.extractCookieToken(request,cookieName(name)); }

    private void writeNamed(HttpServletResponse response, String name, String value, int maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name,value)
                .httpOnly(true).secure(properties.isSecure()).sameSite("Lax").path("/").maxAge(maxAge).build().toString());
    }

    public void clear(HttpServletResponse response) {
        write(response,ACCESS,"",0); write(response,REFRESH,"",0);
        for (String name : new String[]{"accessToken","refreshToken"}) writeNamed(response,name,"",0);
        if (properties.isSecure()) { writeNamed(response,ACCESS,"",0); writeNamed(response,REFRESH,"",0); }
    }
}
