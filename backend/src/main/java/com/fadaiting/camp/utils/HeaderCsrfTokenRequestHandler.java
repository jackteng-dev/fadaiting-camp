package com.fadaiting.camp.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

public class HeaderCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
    @Override public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
        return request.getHeader(token.getHeaderName());
    }
}
