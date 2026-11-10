package com.fadaiting.camp.utils;

import com.fadaiting.camp.config.EcpayProperties;
import com.fadaiting.camp.config.LineBotProperties;
import com.fadaiting.camp.config.LinePayProperties;
import com.fadaiting.camp.config.AuthCookieProperties;
import com.fadaiting.camp.config.LineIdentityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;

@EnableWebSecurity
@Configuration
@EnableConfigurationProperties({LinePayProperties.class, EcpayProperties.class, LineBotProperties.class, AuthCookieProperties.class, LineIdentityProperties.class})
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(AuthCookieProperties cookies) {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieName(cookies.isSecure() ? "__Host-camp_csrf" : "camp_csrf");
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(cookies.isSecure()).sameSite("Lax").path("/"));
        return repository;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CookieCsrfTokenRepository csrfRepository) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                        // JWT 每次重建 SecurityContext；僅由真正登入／登出操作輪替 CSRF。
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .csrfTokenRequestHandler(new HeaderCsrfTokenRequestHandler())
                        .ignoringRequestMatchers("/api/public/**", "/public/**"))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request,response,error) -> {
                    boolean csrfError = error instanceof CsrfException;
                    response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(csrfError
                            ? "{\"code\":1,\"errorCode\":\"CSRF_INVALID\",\"message\":\"操作驗證已失效，請重新載入後再試\"}"
                            : "{\"code\":1,\"message\":\"無權限操作\"}");
                }))
                .addFilterBefore(jwtAuthenticationFilter, CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
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
                        ).permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
