package com.fadaiting.camp.service;

import com.fadaiting.camp.mapper.AdminUserMapper;
import com.fadaiting.camp.mapper.RoleMapper;
import com.fadaiting.camp.pojo.Permission;
import com.fadaiting.camp.pojo.Role;
import com.fadaiting.camp.utils.CampRolePolicy;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CampAccessService {
    private final AdminUserMapper userMapper;
    private final RoleMapper roleMapper;

    public CampAccessService(AdminUserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    public String currentRole(Integer userId) {
        if (!"F".equals(userMapper.findAccountStatus(userId))) return "";
        return CampRolePolicy.resolve(roleMapper.getUserRole(userId).stream()
                .map(Role::getName).collect(Collectors.toSet()));
    }

    public void requireRole(Integer userId) {
        if (currentRole(userId).isEmpty()) throw new AccessDeniedException("帳號已停用或沒有營區後台角色");
    }

    public boolean isCurrentSession(Integer userId, Object version) {
        Long current = userMapper.findAuthVersion(userId);
        return version instanceof Number number && current != null && current == number.longValue();
    }

    public void invalidateSessions(Integer userId) { userMapper.invalidateSessions(userId); }

    public List<Permission> permissions(Integer userId) {
        String role = currentRole(userId);
        if (role.isEmpty()) return List.of();
        // 延用既有選單格式，但由固定角色決定哪些入口可顯示。
        List<Permission> permissions = new java.util.ArrayList<>(roleMapper.findAllPermission().stream().filter(permission -> {
            if (List.of(100, 106, 141, 146).contains(permission.getId())) {
                return "ADMIN".equals(role) || permission.getId() == 106;
            }
            return permission.getUrl() != null && permission.getMethod() != null
                    && CampRolePolicy.allows(role, permission.getUrl(), permission.getMethod());
        }).toList());
        if ("ADMIN".equals(role)) {
            Permission accounts = new Permission();
            accounts.setId(-1);
            accounts.setName("員工帳號");
            accounts.setParentId(146);
            accounts.setLevel(2);
            accounts.setUrl("/admin/staff");
            accounts.setMethod("GET");
            accounts.setAdminRouter("/admin/staff");
            permissions.add(accounts);
        }
        return permissions;
    }
}
