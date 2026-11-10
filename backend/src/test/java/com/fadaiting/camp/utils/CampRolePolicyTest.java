package com.fadaiting.camp.utils;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CampRolePolicyTest {
    @Test
    void legacyUserAndCustomRolesDoNotGainAccess() {
        assertEquals("ADMIN", CampRolePolicy.resolve(Set.of("admin")));
        assertEquals("STAFF", CampRolePolicy.resolve(Set.of("STAFF")));
        assertEquals("", CampRolePolicy.resolve(Set.of("user", "editor", "staff")));
        assertFalse(CampRolePolicy.allows("", "/booking/list", "POST"));
    }

    @Test
    void staffCanReadBookingsAndOperateWithoutFinancialPrivileges() {
        assertTrue(CampRolePolicy.allows("STAFF", "/booking/list", "POST"));
        assertTrue(CampRolePolicy.allows("STAFF", "/booking/detail/1", "GET"));
        assertTrue(CampRolePolicy.allows("STAFF", "/booking/note", "PATCH"));
        for (String operation : Set.of("PATCH /booking/detail", "POST /payment/manual",
                "POST /payment/list", "POST /notification/send-queued", "PUT /payment/methods",
                "PUT /camp/site", "POST /role/assignUser", "POST /role/assignPermission")) {
            String[] parts = operation.split(" ");
            assertFalse(CampRolePolicy.allows("STAFF", parts[1], parts[0]), operation);
        }
    }

    @Test
    void onlyAdminCanManageStaffAccounts() {
        assertTrue(CampRolePolicy.allows("ADMIN", "/admin/staff", "GET"));
        assertTrue(CampRolePolicy.allows("ADMIN", "/admin/staff", "POST"));
        assertTrue(CampRolePolicy.allows("ADMIN", "/admin/staff/1", "PATCH"));
        assertFalse(CampRolePolicy.allows("STAFF", "/admin/staff", "GET"));
        assertFalse(CampRolePolicy.allows("STAFF", "/admin/staff/1", "PATCH"));
    }

    @Test
    void onlyAdminCanReadQueueAndOperateNotifications() {
        for (String operation : Set.of("GET /notification/queue", "GET /notification/42/attempts", "POST /notification/simulate-queued", "PATCH /notification/status")) {
            String[] parts=operation.split(" "); assertTrue(CampRolePolicy.allows("ADMIN",parts[1],parts[0])); assertFalse(CampRolePolicy.allows("STAFF",parts[1],parts[0]));
        }
    }
    @Test
    void adminDoesNotHaveAnUnrestrictedWildcard() {
        assertTrue(CampRolePolicy.allows("ADMIN", "/payment/manual", "POST"));
        assertTrue(CampRolePolicy.allows("ADMIN", "/camp/zone/42", "DELETE"));
        assertFalse(CampRolePolicy.allows("ADMIN", "/camp/future-dangerous-endpoint", "POST"));
        assertFalse(CampRolePolicy.allows("ADMIN", "/role/assignPermission", "POST"));
        assertFalse(CampRolePolicy.allows("ADMIN", "/booking/detail/1", "DELETE"));
    }
    @Test void onlyAdminCanReadBackupObservationAndNoHttpBackupIsGranted() {
        assertTrue(CampRolePolicy.allows("ADMIN","/admin/backup/status","GET"));
        assertFalse(CampRolePolicy.allows("STAFF","/admin/backup/status","GET"));
        assertFalse(CampRolePolicy.allows("","/admin/backup/status","GET"));
        assertFalse(CampRolePolicy.allows("ADMIN","/admin/backup/status","POST"));
        assertFalse(CampRolePolicy.allows("ADMIN","/admin/backup/restore","POST"));
    }
    @Test void retentionPreviewOnlyGrantsAdminGet() {
        assertTrue(CampRolePolicy.allows("ADMIN","/notification/retention-preview","GET"));
        assertFalse(CampRolePolicy.allows("STAFF","/notification/retention-preview","GET"));
        assertFalse(CampRolePolicy.allows("","/notification/retention-preview","GET"));
        assertFalse(CampRolePolicy.allows("ADMIN","/notification/retention-preview","POST"));
        assertFalse(CampRolePolicy.allows("ADMIN","/notification/retention","DELETE"));
    }
}
