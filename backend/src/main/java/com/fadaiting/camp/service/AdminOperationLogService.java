package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.AdminOperationLogQueryDto;
import com.fadaiting.camp.pojo.AdminOperationLog;
import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AdminOperationLogService {
    void record(AdminOperationLog log);

    void recordLoginAttempt(String username, User user, HttpServletRequest request, boolean success, String message);

    void recordLogout(Integer userId, String username, HttpServletRequest request, boolean success, String message);

    void recordRequest(HttpServletRequest request, HttpServletResponse response, CustomUserDetails currentUser, Exception exception);

    PageBean<AdminOperationLog> getAdminLogs(AdminOperationLogQueryDto queryDto);
}
