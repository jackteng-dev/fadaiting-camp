package com.fadaiting.camp.mapper;

import com.fadaiting.camp.dto.StaffAccountDto;
import com.fadaiting.camp.pojo.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StaffAccountMapper {
    String ACCOUNT_COLUMNS = """
        SELECT u.id, u.username, u.first_name, u.last_name, u.created_time, u.last_login_time,
               u.auth_version, (u.is_stop = 'F') AS enabled,
               CASE WHEN EXISTS (SELECT 1 FROM user_role ur JOIN role r ON r.id = ur.role_id
                                  WHERE ur.user_id = u.id AND r.name IN ('ADMIN', 'admin'))
                    THEN 'ADMIN' ELSE 'STAFF' END AS camp_role
        FROM app_user u
        WHERE EXISTS (SELECT 1 FROM user_role ur JOIN role r ON r.id = ur.role_id
                       WHERE ur.user_id = u.id AND r.name IN ('ADMIN', 'admin', 'STAFF'))
        """;

    // 所有帳號角色／狀態異動共用交易鎖，避免兩位管理員同時移除彼此。
    @Select("SELECT TRUE FROM pg_advisory_xact_lock(20261006, 1)")
    Boolean lockManagement();

    @Select(ACCOUNT_COLUMNS + " AND u.id = #{id}")
    StaffAccountDto findAccount(Integer id);

    @Select(ACCOUNT_COLUMNS + " AND position(#{username} in u.username) > 0 ORDER BY u.id LIMIT #{limit} OFFSET #{offset}")
    List<StaffAccountDto> findAccounts(@Param("username") String username, @Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM app_user u WHERE position(#{username} in u.username) > 0
        AND EXISTS (SELECT 1 FROM user_role ur JOIN role r ON r.id = ur.role_id
                     WHERE ur.user_id = u.id AND r.name IN ('ADMIN', 'admin', 'STAFF'))
        """)
    long countAccounts(String username);

    @Select("""
        SELECT count(*) FROM app_user u WHERE u.is_stop = 'F'
        AND EXISTS (SELECT 1 FROM user_role ur JOIN role r ON r.id = ur.role_id
                     WHERE ur.user_id = u.id AND r.name IN ('ADMIN', 'admin'))
        """)
    long countActiveAdmins();

    @Select("SELECT id FROM role WHERE name = #{name}")
    Integer findRoleId(String name);

    @Insert("""
        INSERT INTO app_user (username, password, first_name, last_name, role, is_stop)
        VALUES (#{username}, #{password}, #{firstName}, #{lastName}, 'A', 'F')
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertAccount(User user);

    @Delete("""
        DELETE FROM user_role WHERE user_id = #{id}
        AND role_id IN (SELECT id FROM role WHERE name IN ('ADMIN', 'admin', 'STAFF'))
        """)
    void clearOperationalRoles(Integer id);

    @Insert("INSERT INTO user_role (user_id, role_id) VALUES (#{id}, #{roleId})")
    void assignRole(@Param("id") Integer id, @Param("roleId") Integer roleId);

    @Update("""
        UPDATE app_user SET is_stop = #{isStop}, auth_version = auth_version + 1, modified_time = now()
        WHERE id = #{id} AND auth_version = #{expectedVersion}
        """)
    int updateState(@Param("id") Integer id, @Param("isStop") String isStop, @Param("expectedVersion") Long expectedVersion);
}
