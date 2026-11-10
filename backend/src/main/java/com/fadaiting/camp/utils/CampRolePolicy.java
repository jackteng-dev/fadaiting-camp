package com.fadaiting.camp.utils;

import org.springframework.util.AntPathMatcher;
import java.util.Set;

/** 單一營區的固定操作界線；資料庫細粒度權限僅保留選單資料。 */
public final class CampRolePolicy {
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final Set<String> SHARED = Set.of(
            "POST /booking/list", "GET /booking/detail/*", "PATCH /booking/stay",
            "PATCH /booking/note", "PATCH /admin/user/password", "PATCH /upload/userAvatar",
            "PUT /admin/user/update", "GET /admin/user/*");
    private static final Set<String> ADMIN = Set.of(
            "POST /booking/expire-unpaid", "PATCH /booking/detail", "PATCH /booking/status", "POST /payment/list", "POST /payment/summary",
            "POST /payment/manual", "GET /payment/methods", "PUT /payment/methods",
            "GET /payment/finance/*", "POST /payment/refunds", "PATCH /payment/refunds/*", "PATCH /payment/reconciliation/*",
            "POST /notification/list", "POST /notification/send-queued",
            "GET /notification/queue", "GET /notification/retention-preview", "GET /notification/*/attempts", "POST /notification/simulate-queued",
            "POST /notification/departure-reminders", "PATCH /notification/status",
            "POST /operation-log/list", "POST /upload/admin-image",
            "GET /admin/staff", "POST /admin/staff", "PATCH /admin/staff/*", "GET /admin/backup/status");
    private static final Set<String> CAMP_READ = Set.of("site", "site/detail", "zone", "zone/list",
            "unit/list", "rate-plan/list", "availability/list", "rental-item/list",
            "announcement", "faq", "gallery");
    private static final Set<String> CAMP_WRITE = Set.of("zone", "unit", "rate-plan", "availability",
            "rental-item", "announcement", "faq", "gallery");

    private CampRolePolicy() { }

    public static String resolve(Set<String> roles) {
        // 僅相容原本的 admin；不將 user 或自訂角色自動提升為員工。
        if (roles.contains("ADMIN") || roles.contains("admin")) return "ADMIN";
        if (roles.contains("STAFF")) return "STAFF";
        return "";
    }

    public static boolean allows(String role, String path, String method) {
        if (!Set.of("ADMIN", "STAFF").contains(role)) return false;
        String operation = method + " " + path;
        if (matches(SHARED, operation)) return true;
        if (!"ADMIN".equals(role)) return false;
        if (matches(ADMIN, operation)) return true;
        if (path.startsWith("/camp/")) {
            String resource = path.substring(6);
            if ("GET".equals(method)) return CAMP_READ.contains(resource);
            if ("PUT".equals(method) && "site".equals(resource)) return true;
            if (Set.of("POST", "PUT").contains(method)) return CAMP_WRITE.contains(resource);
            if ("DELETE".equals(method)) {
                String[] parts = resource.split("/");
                return parts.length == 2 && CAMP_WRITE.contains(parts[0]) && parts[1].matches("[0-9]+");
            }
        }
        return false;
    }

    private static boolean matches(Set<String> operations, String operation) {
        return operations.stream().anyMatch(pattern -> MATCHER.match(pattern, operation));
    }
}
