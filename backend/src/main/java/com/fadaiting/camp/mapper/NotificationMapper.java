package com.fadaiting.camp.mapper;

import com.fadaiting.camp.dto.NotificationAdminRowDto;
import com.fadaiting.camp.dto.NotificationRetentionBucketDto;
import com.fadaiting.camp.pojo.NotificationRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.fadaiting.camp.pojo.NotificationAttempt;
import com.fadaiting.camp.dto.NotificationQueueStatusDto;
import java.util.List;

@Mapper
public interface NotificationMapper {
    @Select("""
        with classified as (
            select case
                when n.error_code='DELIVERY_UNCERTAIN' or exists (
                    select 1 from notification_attempt a where a.notification_id=n.id and a.error_code='DELIVERY_UNCERTAIN')
                    then 'DELIVERY_UNCERTAIN'
                when n.status not in ('simulated','skipped','acknowledged','sent') then 'UNRESOLVED_STATUS'
                when n.queue_managed or n.event_key is not null then 'QUEUE_IDENTITY'
                when n.attempt_count > 0 or exists (select 1 from notification_attempt a where a.notification_id=n.id) then 'ATTEMPT_HISTORY'
                when n.channel not in ('internal','manual') or n.status='sent' or n.delivery_mode='smtp'
                    or n.provider not in ('system','internal','manual') or n.error_code is not null
                    or coalesce(n.error_message,'') <> '' then 'DELIVERY_AUDIT'
                when n.event_type not in ('booking_submitted','admin_new_booking','departure_reminder') or b.paid_amount <> 0
                    or exists (select 1 from payment_record p where p.booking_id=n.booking_id) then 'FINANCIAL_AUDIT'
                when b.id is null or b.status not in ('cancelled','completed') or b.stay_status='checked_in' then 'BOOKING_OPEN'
                when n.next_attempt_at is not null or n.lease_token is not null or n.lease_until is not null then 'SCHEDULE_OR_LEASE'
                when n.created_time is null or n.modified_time is null or b.modified_time is null
                    or b.check_out_date is null or greatest(n.created_time,n.modified_time,n.sent_time,
                        b.modified_time,b.check_out_date::timestamp) >= #{cutoff} then 'RECENT_ACTIVITY'
                else 'CANDIDATE' end as reason,
                n.status, greatest(n.created_time,n.modified_time,n.sent_time,b.modified_time,b.check_out_date::timestamp) as activity_at
            from notification_record n left join booking b on b.id=n.booking_id
        )
        select reason,count(*) as count,
            count(*) filter(where status='simulated') as simulated_count,
            count(*) filter(where status='skipped') as skipped_count,
            count(*) filter(where status='acknowledged') as acknowledged_count,
            min(activity_at) as oldest_activity_at,max(activity_at) as newest_activity_at
        from classified group by reason order by reason
        """)
    List<NotificationRetentionBucketDto> retentionPreview(@Param("cutoff") LocalDateTime cutoff);

    @Insert("""
            insert into notification_record(
                booking_id,
                channel,
                event_type,
                recipient,
                recipient_name,
                subject,
                message,
                status,
                provider,
                error_message,
                payload,
                sent_time,
                created_time,
                modified_time, queue_managed, event_key, attempt_limit, next_attempt_at
            ) values (
                #{bookingId},
                #{channel},
                #{eventType},
                #{recipient},
                #{recipientName},
                #{subject},
                #{message},
                #{status},
                #{provider},
                #{errorMessage},
                #{payload},
                #{sentTime},
                #{createdTime},
                #{modifiedTime}, #{queueManaged}, #{eventKey}, #{attemptLimit}, #{nextAttemptAt}
            ) on conflict (event_key) where event_key is not null do nothing
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotification(NotificationRecord record);

    @Select("""
            <script>
            select
                nr.*,
                b.booking_no,
                b.status as booking_status,
                bg.contact_name,
                bg.contact_phone
            from notification_record nr
            left join booking b on b.id = nr.booking_id
            left join booking_guest bg on bg.booking_id = b.id
            where 1 = 1
            <if test="bookingNo != null and bookingNo != ''">
                and b.booking_no like concat('%', #{bookingNo}, '%')
            </if>
            <if test="eventType != null and eventType != ''">
                and nr.event_type = #{eventType}
            </if>
            <if test="channel != null and channel != ''">
                and nr.channel = #{channel}
            </if>
            <if test="status != null and status != ''">
                and nr.status = #{status}
            </if>
            order by nr.created_time desc, nr.id desc limit #{limit} offset #{offset}
            </script>
            """)
    List<NotificationAdminRowDto> findAdminNotifications(
            @Param("bookingNo") String bookingNo,
            @Param("eventType") String eventType,
            @Param("channel") String channel,
            @Param("status") String status, @Param("limit") int limit, @Param("offset") long offset
    );

    @Select("""
            <script> select count(*)
            from notification_record nr
            left join booking b on b.id = nr.booking_id
            left join booking_guest bg on bg.booking_id = b.id
            where 1 = 1
            <if test="bookingNo != null and bookingNo != ''">
                and b.booking_no like concat('%', #{bookingNo}, '%')
            </if>
            <if test="eventType != null and eventType != ''">
                and nr.event_type = #{eventType}
            </if>
            <if test="channel != null and channel != ''">
                and nr.channel = #{channel}
            </if>
            <if test="status != null and status != ''">
                and nr.status = #{status}
            </if>
            </script>
            """)
    long countAdminNotifications(@Param("bookingNo") String bookingNo,@Param("eventType") String eventType,
        @Param("channel") String channel,@Param("status") String status);

    @Select("""
            select *
            from notification_record
            where id = #{id}
            limit 1
            """)
    NotificationRecord findById(Integer id);

    @Select("select * from notification_record where id = #{id} for update")
    NotificationRecord findForUpdate(Integer id);

    @Select(value="""
        with candidate as (
            select n.id from notification_record n join booking b on b.id=n.booking_id
            where n.queue_managed and n.channel='email' and n.status in ('queued','retry_wait')
                and n.next_attempt_at <= #{now} and n.attempt_count < n.attempt_limit
                and (n.delivery_mode is null or n.delivery_mode=#{mode})
                and (#{bookingNo}::text is null or b.booking_no=#{bookingNo})
            order by n.next_attempt_at,n.id for update of n skip locked limit 1
        )
        update notification_record n set status='processing', attempt_count=n.attempt_count+1,
            delivery_mode=#{mode}, lease_token=#{token}, lease_until=#{leaseUntil},
            next_attempt_at=null, operation_version=n.operation_version+1, modified_time=now()
        from candidate c where n.id=c.id returning n.*
        """, affectData=true)
    NotificationRecord claim(@Param("now") OffsetDateTime now, @Param("leaseUntil") OffsetDateTime leaseUntil,
        @Param("token") UUID token, @Param("mode") String mode, @Param("bookingNo") String bookingNo);

    @Insert("""
        insert into notification_attempt(notification_id,attempt_number,lease_token,delivery_mode,started_at,actor_user_id,actor_name)
        values(#{record.id},#{record.attemptCount},#{record.leaseToken},#{record.deliveryMode},#{now},#{actorId},#{actorName})
        """)
    void insertAttempt(@Param("record") NotificationRecord record, @Param("now") OffsetDateTime now,
        @Param("actorId") Integer actorId, @Param("actorName") String actorName);

    @Update("""
        update notification_record set status=#{status}, error_code=#{code}, error_message=#{message},
            next_attempt_at=#{next}, sent_time=#{sent}, lease_token=null,lease_until=null,
            operation_version=operation_version+1,modified_time=now()
        where id=#{id} and lease_token=#{token} and status='processing'
        """)
    int finish(@Param("id") Integer id, @Param("token") UUID token, @Param("status") String status,
        @Param("code") String code, @Param("message") String message, @Param("next") OffsetDateTime next, @Param("sent") LocalDateTime sent);

    @Update("update notification_attempt set status=#{status},error_code=#{code},finished_at=#{now} where lease_token=#{token} and status='processing'")
    int finishAttempt(@Param("token") UUID token,@Param("status") String status,@Param("code") String code,@Param("now") OffsetDateTime now);

    @Select("""
        select * from notification_record where queue_managed and status='processing' and lease_until <= #{now}
        order by lease_until,id for update skip locked limit #{limit}
        """)
    List<NotificationRecord> findExpiredLeases(@Param("now") OffsetDateTime now,@Param("limit") int limit);

    @Update("""
        update notification_record set status=#{status}, error_code=null,error_message=null,
            next_attempt_at=#{next}, attempt_limit=#{attemptLimit},operation_version=operation_version+1,modified_time=now()
        where id=#{id} and operation_version=#{version}
        """)
    int manualUpdate(@Param("id") int id,@Param("version") long version,@Param("status") String status,
        @Param("next") OffsetDateTime next,@Param("attemptLimit") int attemptLimit);

    @Select("""
        select id,notification_id,attempt_number,delivery_mode,status,error_code,started_at,finished_at,actor_user_id,actor_name
        from notification_attempt where notification_id=#{id} order by attempt_number desc limit 100
        """)
    List<NotificationAttempt> findAttempts(Integer id);

    @Select("""
        select count(*) filter(where status in ('queued','retry_wait') and next_attempt_at <= #{now}
                and attempt_count < attempt_limit) as due_count,
            count(*) filter(where status='retry_wait' and next_attempt_at > #{now}
                and attempt_count < attempt_limit) as waiting_count,
            count(*) filter(where status='processing') as processing_count,
            count(*) filter(where status='failed') as failed_count,
            count(*) filter(where status='failed' and error_code='DELIVERY_UNCERTAIN') as uncertain_count,
            count(*) filter(where status='processing' and lease_until <= #{now}) as expired_lease_count,
            count(*) filter(where status in ('queued','retry_wait') and next_attempt_at <= #{overdueBefore}
                and attempt_count < attempt_limit) as overdue_count,
            count(*) filter(where (status in ('queued','retry_wait') and
                (next_attempt_at is null or attempt_count >= attempt_limit)) or
                (status='processing' and (lease_until is null or lease_token is null))) as blocked_count,
            min(next_attempt_at) filter(where status in ('queued','retry_wait') and next_attempt_at <= #{now}
                and attempt_count < attempt_limit) as oldest_due_at,
            min(lease_until) filter(where status='processing' and lease_until <= #{now}) as oldest_expired_lease_at
        from notification_record where queue_managed and channel='email'
        """)
    NotificationQueueStatusDto queueStatus(@Param("now") OffsetDateTime now,
            @Param("overdueBefore") OffsetDateTime overdueBefore);
}
