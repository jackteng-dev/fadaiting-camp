package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.LineUserBinding;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LineUserBindingMapper {
    @Insert("""
            insert into line_user_binding(
                line_user_id,
                display_name,
                picture_url,
                booking_id,
                booking_no,
                contact_name,
                contact_phone,
                contact_email,
                status,
                bind_token,
                bind_token_expire_time,
                bound_at,
                last_interaction_time,
                created_time,
                modified_time
            ) values (
                #{lineUserId},
                #{displayName},
                #{pictureUrl},
                #{bookingId},
                #{bookingNo},
                #{contactName},
                #{contactPhone},
                #{contactEmail},
                #{status},
                #{bindToken},
                #{bindTokenExpireTime},
                #{boundAt},
                #{lastInteractionTime},
                #{createdTime},
                #{modifiedTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(LineUserBinding binding);

    @Select("""
            select *
            from line_user_binding
            where bind_token = #{bindToken}
            order by id desc
            limit 1
            """)
    LineUserBinding findByBindToken(String bindToken);

    @Select("select * from line_user_binding where id = #{id} for update")
    LineUserBinding findForUpdate(Integer id);

    @Select("select * from line_user_binding where booking_id = #{bookingId} and status = 'active' and identity_verified_at is not null limit 1")
    LineUserBinding findVerifiedByBooking(Integer bookingId);

    @Select("select * from line_user_binding where booking_id = #{bookingId} and line_user_id = #{lineUserId} and status = 'active' and identity_verified_at is not null limit 1")
    LineUserBinding findVerifiedForUserBooking(@Param("lineUserId") String lineUserId, @Param("bookingId") Integer bookingId);

    @Update("update line_user_binding set status = 'inactive', modified_time = now() where booking_id = #{bookingId} and status = 'active' and identity_verified_at is null")
    void deactivateUnverifiedForBooking(Integer bookingId);

    @Select("""
            select *
            from line_user_binding
            where line_user_id = #{lineUserId}
              and status = 'active'
              and identity_verified_at is not null
            order by bound_at desc nulls last, id desc
            limit 1
            """)
    LineUserBinding findActiveByLineUserId(String lineUserId);

    @Update("""
            update line_user_binding
            set status = #{status},
                modified_time = now()
            where line_user_id = #{lineUserId}
              and status = 'active'
            """)
    void updateActiveStatusByLineUserId(@Param("lineUserId") String lineUserId, @Param("status") String status);

    @Update("""
            update line_user_binding
            set line_user_id = #{lineUserId},
                display_name = #{displayName},
                picture_url = #{pictureUrl},
                status = #{status},
                bind_token = #{bindToken},
                bind_token_expire_time = #{bindTokenExpireTime},
                bound_at = #{boundAt},
                last_interaction_time = #{lastInteractionTime},
                modified_time = #{modifiedTime}
                , identity_verified_at = #{identityVerifiedAt}
            where id = #{id}
            """)
    void updateBinding(LineUserBinding binding);
}
