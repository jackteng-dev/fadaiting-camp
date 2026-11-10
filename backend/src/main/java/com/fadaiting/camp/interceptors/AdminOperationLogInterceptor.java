package com.fadaiting.camp.interceptors;

import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.service.AdminOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class AdminOperationLogInterceptor implements HandlerInterceptor {
    @Autowired
    private AdminOperationLogService adminOperationLogService;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> excludePatterns = List.of(
            "/api/admin/login",
            "/api/admin/logout",
            "/api/auth/**",
            "/api/public/**",
            "/admin/login",
            "/admin/logout",
            "/auth/**",
            "/public/**"
    );

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String path = normalizePath(request);
        if (!isAdminApi(path)) {
            return;
        }
        if (excludePatterns.stream().anyMatch(pattern -> matcher.match(pattern, path))) {
            return;
        }

        adminOperationLogService.recordRequest(request, response, resolveCurrentUser(), ex);
    }

    private CustomUserDetails resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentUser)) {
            return null;
        }
        return currentUser;
    }

    private boolean isAdminApi(String path) {
        return path.startsWith("/api/admin")
                || path.startsWith("/api/booking")
                || path.startsWith("/api/camp")
                || path.startsWith("/api/payment")
                || path.startsWith("/api/notification")
                || path.startsWith("/api/operation-log")
                || path.startsWith("/api/upload")
                || path.startsWith("/api/role")
                || path.startsWith("/api/category")
                || path.startsWith("/api/article")
                || path.startsWith("/admin")
                || path.startsWith("/booking")
                || path.startsWith("/camp")
                || path.startsWith("/payment")
                || path.startsWith("/notification")
                || path.startsWith("/operation-log")
                || path.startsWith("/upload")
                || path.startsWith("/role")
                || path.startsWith("/category")
                || path.startsWith("/article");
    }

    private String normalizePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
