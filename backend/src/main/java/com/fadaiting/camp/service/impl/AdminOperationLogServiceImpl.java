package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.dto.AdminOperationLogQueryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.mapper.AdminOperationLogMapper;
import com.fadaiting.camp.pojo.AdminOperationLog;
import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.User;
import com.fadaiting.camp.service.AdminOperationLogService;
import com.fadaiting.camp.utils.ListPageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@Service
public class AdminOperationLogServiceImpl implements AdminOperationLogService {
    private static final int TEXT_LIMIT = 1000;

    @Autowired
    private AdminOperationLogMapper adminOperationLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AdminOperationLog log) {
        if (log == null) {
            return;
        }
        log.setUsername(limit(safeText(log.getUsername()), 80));
        log.setActionType(limit(safeText(log.getActionType()), 40));
        log.setActionLabel(limit(safeText(log.getActionLabel()), 160));
        log.setRequestMethod(limit(safeText(log.getRequestMethod()), 20));
        log.setRequestPath(limit(safeText(log.getRequestPath()), 255));
        log.setQueryString(limit(safeText(log.getQueryString()), TEXT_LIMIT));
        log.setMessage(limit(safeText(log.getMessage()), TEXT_LIMIT));
        log.setIpAddress(limit(safeText(log.getIpAddress()), 80));
        log.setUserAgent(limit(safeText(log.getUserAgent()), 255));
        log.setCreatedTime(log.getCreatedTime() == null ? LocalDateTime.now() : log.getCreatedTime());
        adminOperationLogMapper.insert(log);
    }

    @Override
    public void recordLoginAttempt(String username, User user, HttpServletRequest request, boolean success, String message) {
        AdminOperationLog log = baseLog(request);
        log.setUserId(user == null ? null : user.getId());
        log.setUsername(user == null ? username : user.getUsername());
        log.setActionType("login");
        log.setActionLabel("登入");
        log.setRequestMethod("POST");
        log.setRequestPath(normalizePath(request));
        log.setStatusCode(success ? 200 : 401);
        log.setSuccess(success);
        log.setMessage(message);
        recordSafely(log);
    }

    @Override
    public void recordLogout(Integer userId, String username, HttpServletRequest request, boolean success, String message) {
        AdminOperationLog log = baseLog(request);
        log.setUserId(userId);
        log.setUsername(username);
        log.setActionType("logout");
        log.setActionLabel("登出");
        log.setRequestMethod("POST");
        log.setRequestPath(normalizePath(request));
        log.setStatusCode(success ? 200 : 401);
        log.setSuccess(success);
        log.setMessage(message);
        recordSafely(log);
    }

    @Override
    public void recordRequest(HttpServletRequest request, HttpServletResponse response, CustomUserDetails currentUser, Exception exception) {
        AdminOperationLog log = baseLog(request);
        String path = normalizePath(request);
        String method = request.getMethod();
        Integer statusCode = response == null ? null : response.getStatus();
        Boolean resultSuccess = (Boolean) request.getAttribute("adminAudit.success");
        String resultMessage = (String) request.getAttribute("adminAudit.message");

        log.setUserId(currentUser == null ? null : currentUser.getId());
        log.setUsername(currentUser == null ? "" : currentUser.getUsername());
        String actionType = resolveActionType(method, path);
        String actionLabel = resolveActionLabel(method, path, request);
        log.setActionType(actionType);
        log.setActionLabel(actionLabel);
        log.setRequestMethod(method);
        log.setRequestPath(path);
        log.setQueryString(request.getQueryString());
        log.setStatusCode(statusCode);
        log.setSuccess(resolveSuccess(statusCode, resultSuccess, exception));
        log.setMessage(resolveMessage(resultMessage, exception, statusCode, actionLabel, log.getSuccess()));
        recordSafely(log);
    }

    @Override
    public PageBean<AdminOperationLog> getAdminLogs(AdminOperationLogQueryDto queryDto) {
        List<AdminOperationLog> logs = adminOperationLogMapper.findAdminLogs(
                normalize(queryDto.getUsername()),
                normalize(queryDto.getActionType()),
                queryDto.getSuccess(),
                normalize(queryDto.getRequestPath()),
                normalize(queryDto.getStartDate()),
                normalize(queryDto.getEndDate())
        );
        ListPageUtil<AdminOperationLog> pageUtil = new ListPageUtil<>(
                logs,
                queryDto.getPageNum(),
                queryDto.getPageSize()
        );
        return new PageBean<>((long) pageUtil.getTotalCount(), pageUtil.getPageList());
    }

    private void recordSafely(AdminOperationLog log) {
        try {
            record(log);
        } catch (Exception e) {
            System.err.println("後台操作紀錄寫入失敗：" + e.getMessage());
        }
    }

    private AdminOperationLog baseLog(HttpServletRequest request) {
        AdminOperationLog log = new AdminOperationLog();
        log.setIpAddress(resolveIp(request));
        log.setUserAgent(request == null ? "" : request.getHeader("User-Agent"));
        log.setCreatedTime(LocalDateTime.now());
        return log;
    }

    private String normalizePath(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String uri = request.getRequestURI();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri.startsWith("/api/")) {
            uri = uri.substring(4);
        }
        return uri;
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String resolveActionType(String method, String path) {
        if ("GET".equalsIgnoreCase(method)) return "view";
        if ("DELETE".equalsIgnoreCase(method)) return "delete";
        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) return "update";
        if ("POST".equalsIgnoreCase(method) && (path.contains("/list") || path.contains("/summary"))) return "view";
        if ("POST".equalsIgnoreCase(method)) return "create";
        return "operate";
    }

    private String resolveActionLabel(String method, String path, HttpServletRequest request) {
        String actionType = resolveActionType(method, path);
        String target = resolveTargetLabel(path);
        String detail = resolveTargetDetail(path, request);
        String prefix;
        if ("view".equals(actionType)) {
            prefix = "查看";
        } else if ("create".equals(actionType)) {
            prefix = "新增 / 執行";
        } else if ("update".equals(actionType)) {
            prefix = "更新";
        } else if ("delete".equals(actionType)) {
            prefix = "刪除";
        } else {
            prefix = "操作";
        }
        if (StringUtils.hasText(detail)) {
            return prefix + target + "：" + detail;
        }
        return prefix + target;
    }

    private String resolveTargetLabel(String path) {
        if (path.startsWith("/admin/staff")) return "員工帳號";
        if (path.startsWith("/admin/user/password")) return "修改密碼";
        if (path.startsWith("/admin/user/update")) return "個人基本資料";
        if (path.startsWith("/admin/user/")) return "後台使用者資料";
        if (path.startsWith("/booking/detail")) return path.matches("/booking/detail/\\d+") ? "預約明細" : "預約處理資訊";
        if (path.startsWith("/booking/status")) return "預約狀態";
        if (path.startsWith("/booking/list")) return "預約列表";
        if (path.startsWith("/booking")) return "預約資料";
        if (path.startsWith("/camp/site")) return "營區基本資料";
        if (path.startsWith("/camp/zone/list")) return "營區區域列表";
        if (path.startsWith("/camp/zone")) return "營區區域";
        if (path.startsWith("/camp/unit/list")) return "營位列表";
        if (path.startsWith("/camp/unit")) return "營位";
        if (path.startsWith("/camp/rate-plan/list")) return "價格方案列表";
        if (path.startsWith("/camp/rate-plan")) return "價格方案";
        if (path.startsWith("/camp/availability/list")) return "每日庫存列表";
        if (path.startsWith("/camp/availability")) return "每日庫存";
        if (path.startsWith("/camp/rental-item/list")) return "租借品列表";
        if (path.startsWith("/camp/rental-item")) return "租借品";
        if (path.startsWith("/camp/announcement")) return "公告";
        if (path.startsWith("/camp/faq")) return "FAQ";
        if (path.startsWith("/camp/gallery")) return "相簿圖片";
        if (path.startsWith("/payment/manual")) return "人工付款";
        if (path.startsWith("/payment/methods")) return "支付方式設定";
        if (path.startsWith("/payment/summary")) return "付款摘要";
        if (path.startsWith("/payment/list")) return "付款紀錄";
        if (path.startsWith("/payment")) return "付款資料";
        if (path.startsWith("/notification/list")) return "通知紀錄";
        if (path.startsWith("/notification/status")) return "通知狀態";
        if (path.startsWith("/notification/simulate-queued")) return "模擬通知處理";
        if (path.startsWith("/notification/send-queued")) return "待寄通知";
        if (path.startsWith("/notification/departure-reminders")) return "出發前提醒";
        if (path.startsWith("/operation-log/list")) return "操作紀錄列表";
        if (path.startsWith("/upload/admin-image")) return "後台圖片";
        if (path.startsWith("/upload/userAvatar")) return "個人頭像";
        if (path.startsWith("/role")) return "角色權限資料";
        if (path.startsWith("/category")) return "分類資料";
        if (path.startsWith("/article")) return "文章資料";
        return "後台功能";
    }

    private String resolveTargetDetail(String path, HttpServletRequest request) {
        JsonNode body = readBody(request);
        String bodyDetail = detailFromBody(path, body);
        if (StringUtils.hasText(bodyDetail)) {
            return bodyDetail;
        }
        String idFromPath = trailingId(path);
        if (StringUtils.hasText(idFromPath)) {
            return "#" + idFromPath;
        }
        return "";
    }

    private String detailFromBody(String path, JsonNode body) {
        if (body == null || body.isMissingNode() || body.isNull()) {
            return "";
        }
        if (body.isArray()) {
            return detailFromArray(body);
        }
        if (path.startsWith("/booking/status")) {
            return joinNonBlank("訂單 #" + text(body, "bookingId"), statusText(text(body, "status")));
        }
        if (path.startsWith("/booking/detail")) {
            return "訂單 #" + text(body, "bookingId");
        }
        if (path.startsWith("/payment/manual")) {
            return joinNonBlank("訂單 #" + text(body, "bookingId"), "金額 " + text(body, "amount"));
        }
        if (path.startsWith("/notification/status")) {
            return joinNonBlank("通知 #" + text(body, "notificationId"), statusText(text(body, "status")));
        }
        if (path.startsWith("/notification/departure-reminders")) {
            return joinNonBlank(text(body, "targetDate"), text(body, "daysBefore") + " 天前");
        }
        return firstText(body, "name", "title", "question", "displayName", "bookingNo", "username", "imageUrl", "id");
    }

    private String detailFromArray(JsonNode body) {
        StringBuilder builder = new StringBuilder();
        Iterator<JsonNode> iterator = body.elements();
        while (iterator.hasNext() && builder.length() < 120) {
            JsonNode item = iterator.next();
            String text = firstText(item, "displayName", "name", "title", "question", "provider", "id");
            if (!StringUtils.hasText(text)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("、");
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private JsonNode readBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        String body = new String(content, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(body) || !body.trim().startsWith("{") && !body.trim().startsWith("[")) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(node, fieldName);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return "";
        }
        return node.get(fieldName).asText("");
    }

    private String statusText(String status) {
        if (!StringUtils.hasText(status)) return "";
        return switch (status) {
            case "pending" -> "待確認";
            case "confirmed" -> "已確認";
            case "cancelled" -> "已取消";
            case "paid" -> "已付款";
            case "failed" -> "失敗";
            case "sent" -> "已發送";
            case "skipped" -> "略過";
            case "queued" -> "待發送";
            default -> status;
        };
    }

    private String joinNonBlank(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!StringUtils.hasText(value) || value.endsWith("#") || value.endsWith(" ")) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("，");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String trailingId(String path) {
        int slashIndex = path.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex >= path.length() - 1) {
            return "";
        }
        String value = path.substring(slashIndex + 1);
        return value.matches("\\d+") ? value : "";
    }

    private boolean resolveSuccess(Integer statusCode, Boolean resultSuccess, Exception exception) {
        if (exception != null) {
            return false;
        }
        if (resultSuccess != null) {
            return resultSuccess;
        }
        return statusCode == null || statusCode < 400;
    }

    private String resolveMessage(String resultMessage, Exception exception, Integer statusCode, String actionLabel, boolean success) {
        if (exception != null && StringUtils.hasText(exception.getMessage())) {
            return exception.getMessage();
        }
        if (StringUtils.hasText(resultMessage)
                && !"操作成功".equals(resultMessage)
                && !"操作失敗".equals(resultMessage)) {
            return resultMessage;
        }
        if (StringUtils.hasText(actionLabel)) {
            return actionLabel + (success ? "成功" : "失敗");
        }
        return statusCode == null ? "" : "HTTP " + statusCode;
    }

    private String normalize(String value) {
        String normalized = safeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String limit(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
