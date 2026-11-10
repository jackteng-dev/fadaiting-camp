package com.fadaiting.camp;

import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.expection.AccountConflictException;
import com.fadaiting.camp.mapper.StaffAccountMapper;
import com.fadaiting.camp.pojo.User;
import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.service.impl.StaffAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffAccountServiceTest {
    private final StaffAccountMapper mapper = mock(StaffAccountMapper.class);
    private final CampAccessService access = mock(CampAccessService.class);
    private final BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);
    private final StaffAccountServiceImpl service = new StaffAccountServiceImpl(mapper, access, encoder);

    @BeforeEach
    void setup() {
        when(access.currentRole(1)).thenReturn("ADMIN");
        when(mapper.findRoleId("ADMIN")).thenReturn(3);
        when(mapper.findRoleId("STAFF")).thenReturn(4);
        when(mapper.findAccount(1)).thenReturn(account("ADMIN", true, 0));
        when(mapper.countActiveAdmins()).thenReturn(1L);
        when(mapper.updateState(anyInt(), anyString(), anyLong())).thenReturn(1);
    }

    private StaffAccountDto account(String role, boolean enabled, long version) {
        StaffAccountDto account = new StaffAccountDto();
        account.setId(1); account.setUsername("synthetic_admin");
        account.setCampRole(role); account.setEnabled(enabled); account.setAuthVersion(version);
        return account;
    }

    private StaffAccountUpdateRequest update(String role, boolean enabled, long version) {
        StaffAccountUpdateRequest request = new StaffAccountUpdateRequest();
        request.setCampRole(role); request.setEnabled(enabled); request.setExpectedVersion(version);
        return request;
    }

    @Test
    void lastAdminCannotBeDisabledOrDemoted() {
        assertThrows(AccountConflictException.class, () -> service.update(1, 1, update("ADMIN", false, 0)));
        assertThrows(AccountConflictException.class, () -> service.update(1, 1, update("STAFF", true, 0)));
        verify(mapper, never()).updateState(anyInt(), anyString(), anyLong());
        verify(mapper, never()).clearOperationalRoles(anyInt());
    }

    @Test
    void anotherActiveAdminAllowsDemotion() {
        when(mapper.countActiveAdmins()).thenReturn(2L);
        service.update(1, 1, update("STAFF", true, 0));
        verify(mapper).updateState(1, "F", 0L);
        verify(mapper).clearOperationalRoles(1);
        verify(mapper).assignRole(1, 4);
    }

    @Test
    void enablingInactiveAdminDoesNotTriggerRemovalGuard() {
        when(mapper.findAccount(2)).thenReturn(account("ADMIN", false, 0));
        service.update(1, 2, update("ADMIN", true, 0));
        verify(mapper).updateState(2, "F", 0L);
    }

    @Test
    void noChangeDoesNotRevokeSessions() {
        service.update(1, 1, update("ADMIN", true, 0));
        verify(mapper, never()).updateState(anyInt(), anyString(), anyLong());
    }

    @Test
    void stalePageCannotOverwriteNewRole() {
        assertThrows(AccountConflictException.class, () -> service.update(1, 1, update("STAFF", true, 9)));
        verify(mapper, never()).updateState(anyInt(), anyString(), anyLong());
    }

    @Test
    void revocationWhileWaitingForLockCannotBeBypassed() {
        when(access.currentRole(1)).thenReturn("STAFF");
        assertThrows(AccessDeniedException.class, () -> service.update(1, 1, update("STAFF", true, 0)));
        var order = inOrder(mapper, access);
        order.verify(mapper).lockManagement();
        order.verify(access).currentRole(1);
        verify(mapper, never()).updateState(anyInt(), anyString(), anyLong());
    }

    @Test
    void arbitraryRolesCannotBeAssigned() {
        assertThrows(IllegalArgumentException.class, () -> service.update(1, 1, update("user", true, 0)));
        verify(mapper, never()).updateState(anyInt(), anyString(), anyLong());
    }

    @Test
    void concurrentSessionChangeProducesConflictBeforeRoleMutation() {
        when(mapper.findAccount(2)).thenReturn(account("STAFF", true, 0));
        when(mapper.updateState(2, "T", 0L)).thenReturn(0);
        assertThrows(AccountConflictException.class, () -> service.update(1, 2, update("STAFF", false, 0)));
        verify(mapper, never()).clearOperationalRoles(anyInt());
    }

    @Test
    void passwordMismatchDoesNotCreateUser() {
        StaffAccountCreateRequest request = new StaffAccountCreateRequest();
        request.setPassword("Synthetic123!"); request.setConfirmPassword("Different123!");
        assertThrows(IllegalArgumentException.class, () -> service.create(1, request));
        verify(mapper, never()).insertAccount(any());
    }

    @Test
    void duplicateUsernameIsReportedWithoutAssigningRoles() {
        StaffAccountCreateRequest request = new StaffAccountCreateRequest();
        request.setUsername("synthetic_staff"); request.setPassword("Synthetic123!");
        request.setConfirmPassword("Synthetic123!"); request.setCampRole("STAFF");
        when(encoder.encode(anyString())).thenReturn("bcrypt-hash");
        doThrow(new DuplicateKeyException("duplicate")).when(mapper).insertAccount(any(User.class));
        assertThrows(AccountConflictException.class, () -> service.create(1, request));
        verify(mapper, never()).assignRole(anyInt(), anyInt());
    }
}
