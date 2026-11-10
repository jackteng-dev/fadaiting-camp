package com.fadaiting.camp.controller;

import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.service.SessionCookieService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.pojo.JwtToken;
import com.fadaiting.camp.pojo.Permission;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.pojo.User;
import com.fadaiting.camp.service.AdminUserService;
import com.fadaiting.camp.service.RoleService;
import com.fadaiting.camp.service.UserService;
import com.fadaiting.camp.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class AuthenticationController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CampAccessService campAccessService;

    @Autowired private SessionCookieService sessionCookies;

    @GetMapping("/auth/csrf")
    public Result<Map<String,String>> csrf(CsrfToken token, HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        return Result.success(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));
    }

    @GetMapping("/auth/me")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        String token = sessionCookies.extract(request, SessionCookieService.ACCESS);
        if (token == null || !JwtUtil.validate(token)) {
            return Result.error("未登入，請先登入");
        }

        Map<String, Object> claims = JwtUtil.parseToken(token);
        String userId = String.valueOf(claims.get("id"));

        // 從 Redis 拿 user 資料
        String userJson = stringRedisTemplate.opsForValue().get("userInfo:" + userId);


        if (userJson == null || !campAccessService.isCurrentSession(Integer.valueOf(userId), claims.get("authVersion"))
                || campAccessService.currentRole(Integer.valueOf(userId)).isEmpty()
                || !"access".equals(claims.get("token_type"))) {
            return Result.error("登入逾時或資料異常，請重新登入");
        }

        try {
            // 將 JSON 還原成 User + List<Permission>
            User user = objectMapper.readValue(userJson, User.class);
            user.setPermissions(campAccessService.permissions(Integer.valueOf(userId)));
            user.setCampRole(campAccessService.currentRole(Integer.valueOf(userId)));

            return Result.success(user);
        } catch (JsonProcessingException e) {
            return Result.error("資料解析錯誤");
        }
    }


    @PostMapping("/auth/refresh")
    public Result<Void> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("重刷token");

        String refreshToken = sessionCookies.extract(request, SessionCookieService.REFRESH);
        if (refreshToken == null || !JwtUtil.validate(refreshToken)) {
            return Result.error("無效的刷新令牌");
        }

        Map<String, Object> claims = JwtUtil.parseToken(refreshToken);
        String userId = String.valueOf(claims.get("id"));
        if (!"refresh".equals(claims.get("token_type")) ||
                !campAccessService.isCurrentSession(Integer.valueOf(userId), claims.get("authVersion")) ||
                campAccessService.currentRole(Integer.valueOf(userId)).isEmpty() ||
                !Boolean.TRUE.equals(stringRedisTemplate.hasKey("userInfo:" + userId))) {
            return Result.error("登入狀態已失效，請重新登入");
        }
        String deviceId = String.valueOf(claims.get("deviceId"));
        String jti = String.valueOf(claims.get("jti"));
        String rememberMe = String.valueOf(claims.get("rememberMe"));

        String refreshTokenKey = "refreshToken:" + userId + ":" + deviceId;
        String storedJti = stringRedisTemplate.opsForValue().get(refreshTokenKey);

        if (!jti.equals(storedJti)) {
            return Result.error("登入狀態已失效，請重新登入");
        }

        // ✅ 生成新的 jti，新的 Refresh Token
        String newJti = UUID.randomUUID().toString();

        //刷新令牌
        Map<String, Object> newRefreshClaims = Map.of(
                "id", userId,
                "username", claims.get("username"),
                "deviceId",deviceId,
                "token_type", "refresh",
                "authVersion", claims.get("authVersion"),
                "jti",newJti,
                "rememberMe", rememberMe
        );
        String newRefreshToken = JwtUtil.genRefreshToken(newRefreshClaims);

        //訪問令牌
        Map<String, Object> newAccessClaims = Map.of(
                "id", userId,
                "username", claims.get("username"),
                "token_type", "access",
                "authVersion", claims.get("authVersion")
        );
        String newAccessToken = JwtUtil.genAccessToken(newAccessClaims);

        // ✅ 更新 Redis（7 天）
        Long rotated = stringRedisTemplate.execute(new DefaultRedisScript<>(
                "if redis.call('GET',KEYS[1]) ~= ARGV[1] then return 0 end redis.call('SET',KEYS[1],ARGV[2],'EX',ARGV[3]); return 1",Long.class),
                List.of(refreshTokenKey),jti,newJti,"604800");
        if (!Long.valueOf(1).equals(rotated)) return Result.error("登入狀態已失效，請重新登入");

        // ✅ 寫入 Cookie
        int refreshMaxAge = "true".equals(rememberMe)?7 * 24 * 60 * 60:-1;
        sessionCookies.write(response, SessionCookieService.REFRESH, newRefreshToken, refreshMaxAge); // 7 天
        sessionCookies.write(response, SessionCookieService.ACCESS, newAccessToken, "true".equals(rememberMe) ? 15 * 60 : -1); // 15 分鐘

        return Result.success();

    }
}
