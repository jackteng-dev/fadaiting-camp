package com.fadaiting.camp;

import com.fadaiting.camp.mapper.AdminUserMapper;
import com.fadaiting.camp.mapper.RoleMapper;
import com.fadaiting.camp.pojo.Permission;
import com.fadaiting.camp.pojo.Role;
import com.fadaiting.camp.service.CampAccessService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampAccessServiceTest {
    private final AdminUserMapper users = mock(AdminUserMapper.class);
    private final RoleMapper roles = mock(RoleMapper.class);
    private final CampAccessService access = new CampAccessService(users, roles);

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    @Test
    void onlyCurrentDatabaseVersionAuthenticates() {
        when(users.findAuthVersion(1)).thenReturn(2L);
        assertTrue(access.isCurrentSession(1, 2));
        assertFalse(access.isCurrentSession(1, 1));
        assertFalse(access.isCurrentSession(1, null));
        assertFalse(access.isCurrentSession(1, "2"));
    }

    @Test
    void missingAndDisabledAccountsCannotAcquireRoles() {
        when(users.findAccountStatus(1)).thenReturn(null, "T");
        assertEquals("", access.currentRole(1));
        assertEquals("", access.currentRole(1));
        verifyNoInteractions(roles);
    }

    @Test
    void roleIsReadAgainAfterRevocation() {
        when(users.findAccountStatus(1)).thenReturn("F");
        when(roles.getUserRole(1)).thenReturn(List.of(role("ADMIN")), List.of(role("STAFF")), List.of());
        assertEquals("ADMIN", access.currentRole(1));
        assertEquals("STAFF", access.currentRole(1));
        assertEquals("", access.currentRole(1));
    }

    @Test
    void legacyCustomerIsNotConvertedToEmployee() {
        when(users.findAccountStatus(1)).thenReturn("F");
        when(roles.getUserRole(1)).thenReturn(List.of(role("user")));
        assertEquals("", access.currentRole(1));
        assertTrue(access.permissions(1).isEmpty());
    }

    @Test
    void staffMenuCannotContainFinancialActionFromLegacyPermissions() {
        when(users.findAccountStatus(1)).thenReturn("F");
        when(roles.getUserRole(1)).thenReturn(List.of(role("STAFF")));
        Permission payment = new Permission();
        payment.setId(200);
        payment.setUrl("/payment/manual");
        payment.setMethod("POST");
        when(roles.findAllPermission()).thenReturn(List.of(payment));
        assertTrue(access.permissions(1).isEmpty());
    }
}
