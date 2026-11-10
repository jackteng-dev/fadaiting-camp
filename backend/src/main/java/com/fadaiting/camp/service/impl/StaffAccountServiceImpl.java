package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.expection.AccountConflictException;
import com.fadaiting.camp.mapper.StaffAccountMapper;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.User;
import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.service.StaffAccountService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
public class StaffAccountServiceImpl implements StaffAccountService {
    private final StaffAccountMapper mapper;
    private final CampAccessService access;
    private final BCryptPasswordEncoder encoder;

    public StaffAccountServiceImpl(StaffAccountMapper mapper, CampAccessService access, BCryptPasswordEncoder encoder) {
        this.mapper = mapper;
        this.access = access;
        this.encoder = encoder;
    }

    private void requireAdmin(Integer actorId) {
        if (!"ADMIN".equals(access.currentRole(actorId))) throw new AccessDeniedException("僅營區管理員可管理員工帳號");
    }

    private Integer roleId(String role) {
        if (role == null || !Set.of("ADMIN", "STAFF").contains(role)) throw new IllegalArgumentException("不支援的營區角色");
        Integer id = mapper.findRoleId(role);
        if (id == null) throw new AccountConflictException("請先套用固定角色初始化 SQL");
        return id;
    }

    @Override
    public PageBean<StaffAccountDto> list(Integer actorId, String username, int pageNum, int pageSize) {
        requireAdmin(actorId);
        String filter = username == null ? "" : username.trim();
        return new PageBean<>(mapper.countAccounts(filter), mapper.findAccounts(filter, pageSize, (pageNum - 1) * pageSize));
    }

    @Override
    @Transactional
    public StaffAccountDto create(Integer actorId, StaffAccountCreateRequest request) {
        // 密碼雜湊不占用帳號管理交易鎖。
        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("兩次輸入的密碼不一致");
        }
        String password = request.getPassword();
        int groups = (password.matches(".*[a-z].*") ? 1 : 0) + (password.matches(".*[A-Z].*") ? 1 : 0)
                + (password.matches(".*[0-9].*") ? 1 : 0) + (password.matches(".*[^a-zA-Z0-9].*") ? 1 : 0);
        if (groups < 3 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密碼需包含大小寫、數字、符號中的至少三種，且 UTF-8 不超過 72 bytes");
        }
        String hash = encoder.encode(password);
        mapper.lockManagement();
        requireAdmin(actorId);
        Integer roleId = roleId(request.getCampRole());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(hash);
        user.setFirstName(request.getFirstName() == null ? "" : request.getFirstName().trim());
        user.setLastName(request.getLastName() == null ? "" : request.getLastName().trim());
        try { mapper.insertAccount(user); }
        catch (DuplicateKeyException exception) { throw new AccountConflictException("帳號已存在，請改用其他帳號名稱"); }
        mapper.assignRole(user.getId(), roleId);
        return mapper.findAccount(user.getId());
    }

    @Override
    @Transactional
    public StaffAccountDto update(Integer actorId, Integer id, StaffAccountUpdateRequest request) {
        mapper.lockManagement();
        // 取得鎖之後重新核對操作者，避免已被另一位管理員降級的請求繼續寫入。
        requireAdmin(actorId);
        Integer roleId = roleId(request.getCampRole());
        StaffAccountDto account = mapper.findAccount(id);
        if (account == null) throw new IllegalArgumentException("找不到營區員工帳號");
        if (!account.getAuthVersion().equals(request.getExpectedVersion())) {
            throw new AccountConflictException("資料已被其他操作更新，請重新載入後再操作");
        }
        boolean sameRole = account.getCampRole().equals(request.getCampRole());
        if (sameRole && account.getEnabled().equals(request.getEnabled())) return account;
        if (account.getEnabled() && "ADMIN".equals(account.getCampRole())
                && (!request.getEnabled() || !"ADMIN".equals(request.getCampRole()))
                && mapper.countActiveAdmins() <= 1) {
            throw new AccountConflictException("至少需保留一位啟用中的營區管理員");
        }
        if (mapper.updateState(id, request.getEnabled() ? "F" : "T", request.getExpectedVersion()) != 1) {
            throw new AccountConflictException("資料已被其他操作更新，請重新載入後再操作");
        }
        if (!sameRole) {
            mapper.clearOperationalRoles(id);
            mapper.assignRole(id, roleId);
        }
        return mapper.findAccount(id);
    }
}
