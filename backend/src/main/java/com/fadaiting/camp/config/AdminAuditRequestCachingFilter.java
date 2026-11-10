package com.fadaiting.camp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Set;

@Component
public class AdminAuditRequestCachingFilter extends OncePerRequestFilter {
    private static final Set<String> CACHE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int CACHE_LIMIT = 32 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldCache(request) && !(request instanceof ContentCachingRequestWrapper)) {
            filterChain.doFilter(new ContentCachingRequestWrapper(request, CACHE_LIMIT), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldCache(HttpServletRequest request) {
        if (!CACHE_METHODS.contains(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (!uri.contains("/api/")) {
            return false;
        }
        String contentType = request.getContentType();
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return contentType.contains("application/json")
                || contentType.contains("application/x-www-form-urlencoded");
    }
}
