package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.AdminOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminOperationLogMapper {
    @Insert("""
            insert into admin_operation_log(
                user_id,
                username,
                action_type,
                action_label,
                request_method,
                request_path,
                query_string,
                status_code,
                success,
                message,
                ip_address,
                user_agent,
                created_time
            ) values (
                #{userId},
                #{username},
                #{actionType},
                #{actionLabel},
                #{requestMethod},
                #{requestPath},
                #{queryString},
                #{statusCode},
                #{success},
                #{message},
                #{ipAddress},
                #{userAgent},
                #{createdTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AdminOperationLog log);

    @Select("""
            <script>
            select *
            from admin_operation_log
            where 1 = 1
            <if test="username != null and username != ''">
                and username like concat('%', #{username}, '%')
            </if>
            <if test="actionType != null and actionType != ''">
                and action_type = #{actionType}
            </if>
            <if test="success != null">
                and success = #{success}
            </if>
            <if test="requestPath != null and requestPath != ''">
                and (
                    request_path like concat('%', #{requestPath}, '%')
                    or action_label like concat('%', #{requestPath}, '%')
                    or message like concat('%', #{requestPath}, '%')
                )
            </if>
            <if test="startDate != null and startDate != ''">
                and created_time &gt;= cast(#{startDate} as timestamp)
            </if>
            <if test="endDate != null and endDate != ''">
                and created_time &lt; cast(#{endDate} as timestamp) + interval '1 day'
            </if>
            order by created_time desc, id desc
            </script>
            """)
    List<AdminOperationLog> findAdminLogs(
            @Param("username") String username,
            @Param("actionType") String actionType,
            @Param("success") Boolean success,
            @Param("requestPath") String requestPath,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}
