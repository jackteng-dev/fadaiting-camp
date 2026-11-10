package com.fadaiting.camp.controller;

import com.fadaiting.camp.service.CampAccessService;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.dto.AdminPasswordUpdateRequest;
import com.fadaiting.camp.dto.LoginResponseDTO;
import com.fadaiting.camp.dto.UserAccount;
import com.fadaiting.camp.dto.UserRegistrationDto;
import com.fadaiting.camp.pojo.*;
import com.fadaiting.camp.service.AdminUserService;
import com.fadaiting.camp.service.AdminOperationLogService;
import com.fadaiting.camp.service.RoleService;
import com.fadaiting.camp.service.UserService;
import com.fadaiting.camp.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fadaiting.camp.service.SessionCookieService;
import com.fadaiting.camp.service.CsrfSessionService;

@RestController
@RequestMapping("/admin")
@Validated
public class AdminUserController {
    @Autowired
    private AdminUserService adminUserService;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RoleService roleService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AdminOperationLogService adminOperationLogService;

    @Autowired
    private CampAccessService campAccessService;

    @Autowired private SessionCookieService sessionCookies;
    @Autowired private CsrfSessionService csrfSession;

    //管理員 註冊
    @PostMapping("/register")
//    public Result register(@Pattern(regexp = "^\\S{5,16}$") String username, @Pattern(regexp = "^\\S{5,16}$") String password) {
    public Result register(@RequestBody @Valid UserRegistrationDto user) {
        //確認密碼跟確認密碼是否一致
        if (!user.getPassword().equals(user.getRePassword())) {
            return Result.error("確認密碼不相同");
        }
//        System.out.println(user.toString());
        //查詢用戶
        User u = adminUserService.findByUserName(user.getUsername());
        if (u == null) {
            //註冊
            adminUserService.register(user);
            return Result.success();
        } else {
            return Result.error("用戶名已被占用");
        }
    }

    //管理員 登入
    @PostMapping("/login")
    // 密码长度8-20位且至少包含大写字母、小写字母、数字或特殊符号中的任意三种
    public Result<LoginResponseDTO> login(
            @NotEmpty(message = "用戶名不能為空") String username,
            @NotEmpty(message = "密碼不能為空") String password,
            @NotEmpty(message = "裝置ID不能為空") String deviceId,
            String rememberMe,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws JsonProcessingException {
        System.out.println("rememberMe:::" + rememberMe);
        //查詢用戶
        User loginuser = adminUserService.findByUserName(username);

        if (loginuser == null) {
            adminOperationLogService.recordLoginAttempt(username, null, request, false, "用戶名錯誤");
            return Result.error("用戶名錯誤");
        }
        //判斷密碼
        if (!bCryptPasswordEncoder.matches(password, loginuser.getPassword())) {
            adminOperationLogService.recordLoginAttempt(username, loginuser, request, false, "密碼錯誤");
            return Result.error("密碼錯誤");
        }
        List<Permission> permissions = campAccessService.permissions(loginuser.getId());
        loginuser.setCampRole(campAccessService.currentRole(loginuser.getId()));

        if(permissions == null || permissions.isEmpty()){
            adminOperationLogService.recordLoginAttempt(username, loginuser, request, false, "登入失敗：沒有後台權限");
            return Result.error("登入失敗!");
        }

        // ⏱️ 建立 jti（唯一識別）
        String jti = UUID.randomUUID().toString();

        //登入成功
        //刷新令牌
        Map<String, Object> refreshClaims = Map.of(
                "id", loginuser.getId(),
                "username", loginuser.getUsername(),
                "deviceId",deviceId,
                "token_type", "refresh",
                "authVersion", loginuser.getAuthVersion(),
                "jti",jti,
                "rememberMe", rememberMe
        );
        String refreshToken = JwtUtil.genRefreshToken(refreshClaims);
        //把refreshTokenKey存到redis
        String refreshTokenKey = "refreshToken:" + loginuser.getId()+":"+deviceId;
        stringRedisTemplate.opsForValue().set(refreshTokenKey, jti, 7, TimeUnit.DAYS);

        //訪問令牌
        Map<String, Object> accessClaims = Map.of(
                "id", loginuser.getId(),
                "username", loginuser.getUsername(),
                "token_type", "access",
                "authVersion", loginuser.getAuthVersion()
        );
        String accessToken = JwtUtil.genAccessToken(accessClaims);

        //把角色權限存到redis
        String userJson = objectMapper.writeValueAsString(loginuser);
        String userInfoKey = "userInfo:" + loginuser.getId();
        stringRedisTemplate.opsForValue().set(userInfoKey, userJson, 12, TimeUnit.HOURS);

        String permissionsJson = objectMapper.writeValueAsString(permissions);
        String permissionKey = "permissions:" + loginuser.getId();
        stringRedisTemplate.opsForValue().set(permissionKey, permissionsJson, 12, TimeUnit.HOURS);

        adminUserService.updateLoginTime(loginuser.getId());
//                System.out.println(loginResponseDTO);
        //如果有選擇保持登入給他7天，否則瀏覽器結束後消失
        int refreshMaxAge = "true".equals(rememberMe)?7 * 24 * 60 * 60:-1;// 7 天有效
        int accessMaxAge = "true".equals(rememberMe)?15 * 60:-1;// 15 分鐘有效
        sessionCookies.write(response, SessionCookieService.REFRESH, refreshToken, refreshMaxAge);
        sessionCookies.write(response, SessionCookieService.ACCESS, accessToken, accessMaxAge);

        csrfSession.rotate(request,response);
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(null, loginuser);
        adminOperationLogService.recordLoginAttempt(username, loginuser, request, true, "登入成功");

        return Result.success(loginResponseDTO);


    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionCookies.clear(response);
        csrfSession.rotate(request,response);
        // 取出 refreshToken
        String refreshToken = sessionCookies.extract(request, SessionCookieService.REFRESH);
        if (refreshToken == null || !JwtUtil.validate(refreshToken)) {
            adminOperationLogService.recordLogout(null, "", request, false, "登出時沒有有效 refreshToken");
            return Result.success(); // 即使沒 token，也讓使用者成功登出
        }

        // 解析 token
        Map<String, Object> claims = JwtUtil.parseToken(refreshToken);
        String userId = String.valueOf(claims.get("id"));
        String deviceId = String.valueOf(claims.get("deviceId"));
        String username = String.valueOf(claims.get("username"));

        if (!"refresh".equals(claims.get("token_type")) ||
                !campAccessService.isCurrentSession(Integer.valueOf(userId), claims.get("authVersion"))) {
            return Result.success();
        }
        String currentJti = stringRedisTemplate.opsForValue().get("refreshToken:"+userId+":"+deviceId);
        if (currentJti == null || !java.util.Objects.equals(claims.get("jti"),currentJti)) return Result.success();
        campAccessService.invalidateSessions(Integer.valueOf(userId));

        // 移除 Redis 中 refreshToken 記錄
        String refreshKey = "refreshToken:" + userId + ":" + deviceId;
        stringRedisTemplate.delete(refreshKey);

        // 移除權限快取（選擇性，如果你權限不綁裝置的話）
        String permKey = "permissions:" + userId;
        stringRedisTemplate.delete(permKey);

        String userInfoKey = "userInfo:" + userId;
        stringRedisTemplate.delete(userInfoKey);

        // 清除 cookie（accessToken / refreshToken）
        adminOperationLogService.recordLogout(Integer.parseInt(userId), username, request, true, "登出成功");

        return Result.success();
    }

    //查詢所有帳號
    @PostMapping("/account")
    public Result<PageBean<User>> findAllAccount(
            @RequestBody UserAccount userAccount
    ) {
//        System.out.println("pageNum:::" + userAccount.getPageNum());
//        System.out.println("pageSize:::" + userAccount.getPageSize());
//        System.out.println("username:::" + userAccount.getUsername());
//        System.out.println("role:::" + userAccount.getRole());
//        System.out.println("isStop:::" + userAccount.getIsStop());
        PageBean<User> userlist = adminUserService.findAllUser(userAccount.getPageNum(), userAccount.getPageSize(), userAccount.getUsername(), userAccount.getRole(), userAccount.getIsStop());
//        System.out.println(userlist);
        return Result.success(userlist);
    }

    //查詢後台用戶所有資訊
    @GetMapping("/user/{username}")
    public Result<User> findAllByUserName(@PathVariable String username, HttpServletRequest request, HttpServletResponse response) {
        String token = JwtUtil.extractCookieToken(request, "accessToken");
        if (!username.equals(JwtUtil.parseToken(token).get("username"))) {
            throw new AccessDeniedException("僅能查看自己的帳號資料");
        }
        User user = adminUserService.findAllByUserName(username);
        user.setCampRole(campAccessService.currentRole(user.getId()));
        return Result.success(user);
    }

    //查詢所有角色 - 搜尋帳號用
    @GetMapping("/role")
    public Result<List<Role>> getAllRole() {
        return Result.success(roleService.findAllRole());
    }

    //--- "停用"/"啟用" 帳號---
    @PatchMapping("/stopAccount")
    public Result stopAccount(@NotEmpty(message = "操作錯誤") String username, @AuthenticationPrincipal CustomUserDetails currentUser) {
        User user = adminUserService.findByUserName(username);
        if (user.getIsStop() == null) {
            return Result.error("操作錯誤");
        } else if ("T".equals(user.getIsStop())) {
            adminUserService.stopAccount(user.getId(), "F");
        } else if ("F".equals(user.getIsStop())) {
            adminUserService.stopAccount(user.getId(), "T");
        }
        return Result.success();
    }

    //更新用戶訊息
    @PutMapping("/user/update")
    public Result update(@RequestBody @Validated User user,
                         @AuthenticationPrincipal CustomUserDetails currentUser) throws JsonProcessingException {
        if (!currentUser.getId().equals(user.getId()) || !currentUser.getUsername().equals(user.getUsername())) {
            throw new AccessDeniedException("僅能修改自己的帳號資料");
        }
        adminUserService.update(user);
        User updatedUser = adminUserService.findByUserName(user.getUsername());

        String userJson = objectMapper.writeValueAsString(updatedUser);
        String redisKey = "userInfo:" + updatedUser.getId();
        stringRedisTemplate.opsForValue().set(redisKey, userJson, 12, TimeUnit.HOURS);
        return Result.success();
    }

    @PatchMapping("/user/password")
    public Result updatePassword(
            @RequestBody @Validated AdminPasswordUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Integer userId = loginUser.getId();
        String username = loginUser.getUsername();
        User account = adminUserService.findByUserName(username);

        if (account == null || !account.getId().equals(userId)) {
            return Result.error("帳號狀態異常，請重新登入");
        }

        if (!bCryptPasswordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            return Result.error("目前密碼不正確");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Result.error("兩次輸入的新密碼不一致");
        }

        if (bCryptPasswordEncoder.matches(request.getNewPassword(), account.getPassword())) {
            return Result.error("新密碼不能和目前密碼相同");
        }

        adminUserService.updatePwd(request.getNewPassword(), userId);
        campAccessService.invalidateSessions(userId);
        stringRedisTemplate.delete("userInfo:" + userId);
        stringRedisTemplate.delete("permissions:" + userId);
        return Result.success();
    }

}
