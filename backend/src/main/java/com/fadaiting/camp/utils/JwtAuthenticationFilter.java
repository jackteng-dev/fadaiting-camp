package com.fadaiting.camp.utils;

import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.service.SessionCookieService;
import com.fadaiting.camp.pojo.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final CampAccessService accessService;
    private final SessionCookieService sessionCookies;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthenticationFilter(StringRedisTemplate redis, CampAccessService accessService, SessionCookieService sessionCookies) {
        this.redis = redis;
        this.accessService = accessService;
        this.sessionCookies = sessionCookies;
    }

    private static final List<String> WHITE_LIST = List.of(
            "/api/admin/login",
            "/api/admin/logout",
            "/api/auth/refresh",
            "/api/auth/me",
            "/api/auth/csrf",
            "/api/public/camp/**",
            "/api/public/booking/**",
            "/api/public/payment/**",
            "/api/public/line/**",
            "/admin/login",
            "/admin/logout",
            "/auth/refresh",
            "/auth/me",
            "/auth/csrf",
            "/public/camp/**",
            "/public/booking/**",
            "/public/payment/**",
            "/public/line/**"
    );


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (WHITE_LIST.stream().anyMatch(pattern -> matcher.match(pattern, path))) {
            chain.doFilter(request, response);
            return;
        }
        String token = sessionCookies.extract(request, SessionCookieService.ACCESS);
        if (token == null || !JwtUtil.validate(token)) {
            reject(response, 401, "請先登入");
            return;
        }
        Map<String, Object> claims = JwtUtil.parseToken(token);
        if (!"access".equals(claims.get("token_type")) || claims.get("id") == null) {
            reject(response, 401, "無效的訪問令牌");
            return;
        }
        Integer userId;
        try { userId = Integer.valueOf(claims.get("id").toString()); }
        catch (NumberFormatException exception) { reject(response, 401, "無效的訪問令牌"); return; }
        if (!accessService.isCurrentSession(userId, claims.get("authVersion"))) {
            reject(response, 401, "登入已失效，請重新登入");
            return;
        }
        if (!Boolean.TRUE.equals(redis.hasKey("userInfo:" + userId))) {
            reject(response, 401, "登入已失效，請重新登入");
            return;
        }
        // 每次重新查詢帳號狀態與角色；舊 Redis 權限不能保留已撤銷的權力。
        String role = accessService.currentRole(userId);
        String permissionPath = path.startsWith("/api/") ? path.substring(4) : path;
        if (!CampRolePolicy.allows(role, permissionPath, request.getMethod())) {
            reject(response, 403, "無權限操作");
            return;
        }
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var principal = new CustomUserDetails((String) claims.get("username"), userId, authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
