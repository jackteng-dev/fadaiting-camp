package com.fadaiting.camp.mapper;

import com.fadaiting.camp.dto.BookingAdminRowDto;
import com.fadaiting.camp.dto.BookingDetailItemDto;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingGuest;
import com.fadaiting.camp.pojo.BookingItem;
import com.fadaiting.camp.pojo.BookingTimeline;
import com.fadaiting.camp.pojo.CampUnit;
import com.fadaiting.camp.pojo.RatePlan;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Mapper
public interface BookingMapper {
    @Select("""
            select *
            from camp_unit
            where id = #{campUnitId}
              and is_active = true
            """)
    CampUnit findActiveUnitById(Integer campUnitId);

    @Select("""
            select *
            from rate_plan
            where camp_unit_id = #{campUnitId}
              and day_type = #{dayType}
              and is_active = true
            order by id
            limit 1
            """)
    RatePlan findRatePlan(@Param("campUnitId") Integer campUnitId, @Param("dayType") String dayType);

    @Insert("""
            insert into booking(
                booking_no,
                camp_site_id,
                status,
                payment_due_at,
                source,
                check_in_date,
                check_out_date,
                nights,
                guest_count,
                vehicle_count,
                subtotal_amount,
                adjustment_amount,
                total_amount,
                paid_amount,
                note,
                created_time,
                modified_time
            ) values (
                #{bookingNo},
                #{campSiteId},
                #{status},
                #{paymentDueAt},
                #{source},
                #{checkInDate},
                #{checkOutDate},
                #{nights},
                #{guestCount},
                #{vehicleCount},
                #{subtotalAmount},
                #{adjustmentAmount},
                #{totalAmount},
                #{paidAmount},
                #{note},
                #{createdTime},
                #{modifiedTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertBooking(Booking booking);

    @Insert("""
            insert into booking_guest(
                booking_id,
                contact_name,
                contact_phone,
                contact_email,
                arrival_note,
                created_time,
                modified_time
            ) values (
                #{bookingId},
                #{contactName},
                #{contactPhone},
                #{contactEmail},
                #{arrivalNote},
                #{createdTime},
                #{modifiedTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertBookingGuest(BookingGuest bookingGuest);

    @Insert("""
            insert into booking_item(
                booking_id,
                camp_unit_id,
                rate_plan_id,
                rental_item_id,
                item_name,
                quantity,
                unit_price,
                nights,
                line_total,
                created_time,
                modified_time
            ) values (
                #{bookingId},
                #{campUnitId},
                #{ratePlanId},
                #{rentalItemId},
                #{itemName},
                #{quantity},
                #{unitPrice},
                #{nights},
                #{lineTotal},
                #{createdTime},
                #{modifiedTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertBookingItem(BookingItem bookingItem);

    @Select("""
            <script>
            select
                b.id,
                b.booking_no,
                b.status,
                b.source,
                b.stay_status,
                b.checked_in_at,
                b.checked_out_at,
                b.operation_version,
                b.payment_due_at,
                b.expired_at,
                bg.contact_name,
                bg.contact_phone,
                bg.contact_email,
                bi.item_name,
                b.check_in_date,
                b.check_out_date,
                b.nights,
                b.guest_count,
                b.vehicle_count,
                b.subtotal_amount,
                b.adjustment_amount,
                b.total_amount,
                b.paid_amount,
                b.note as booking_note,
                b.admin_note,
                b.confirm_note,
                b.cancel_reason,
                bg.arrival_note,
                bg.emergency_contact_name,
                bg.emergency_contact_phone,
                b.created_time,
                b.modified_time
            from booking b
            left join booking_guest bg on b.id = bg.booking_id
            left join lateral (select string_agg(item_name, '、' order by id) as item_name
                from booking_item where booking_id = b.id) bi on true
            where 1 = 1
            <if test="bookingNo != null and bookingNo != ''">
                and b.booking_no like concat('%', #{bookingNo}, '%')
            </if>
            <if test="status != null and status != ''">
                and b.status = #{status}
            </if>
            <if test="contactName != null and contactName != ''">
                and bg.contact_name like concat('%', #{contactName}, '%')
            </if>
            <if test="startDate != null and endDate != null">
                and ((b.check_in_date &lt;= #{endDate} and b.check_out_date &gt;= #{startDate})
                    or (b.checked_in_at is not null
                        and (b.checked_in_at at time zone 'Asia/Taipei')::date &lt;= #{endDate}
                        and coalesce((b.checked_out_at at time zone 'Asia/Taipei')::date,
                            (current_timestamp at time zone 'Asia/Taipei')::date) &gt;= #{startDate}))
            </if>
            order by b.created_time desc, b.id desc
            </script>
            """)
    List<BookingAdminRowDto> findAdminBookings(
            @Param("bookingNo") String bookingNo,
            @Param("status") String status,
            @Param("contactName") String contactName,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate
    );

    @Select("""
            select *
            from booking
            where id = #{bookingId}
            """)
    Booking findBookingById(Integer bookingId);

    @Select("""
            select id from booking where status = 'pending' and stay_status = 'not_checked_in'
            and paid_amount = 0 and total_amount > 0 and payment_due_at <= #{now}
            and not exists (select 1 from payment_record p where p.booking_id = booking.id and p.status = 'paid' and p.amount > 0)
            order by payment_due_at, id limit #{limit}
            """)
    List<Integer> findDueBookingIds(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    @Select("select * from booking where id = #{bookingId} for update skip locked")
    Booking findBookingForUpdateSkipLocked(Integer bookingId);

    @Update("""
            update booking set status = 'cancelled', expired_at = #{now},
                cancel_reason = '未於付款期限內完成付款', operation_version = operation_version + 1, modified_time = now()
            where id = #{bookingId}
            """)
    void markBookingExpired(@Param("bookingId") Integer bookingId, @Param("now") OffsetDateTime now);

    @Update("update booking set payment_due_at = #{due,jdbcType=TIMESTAMP_WITH_TIMEZONE}, cancel_reason = case when expired_at is not null then null else cancel_reason end, expired_at = null where id = #{bookingId}")
    void resetBookingHold(@Param("bookingId") Integer bookingId, @Param("due") OffsetDateTime due);

    @Select("select * from booking where id = #{bookingId} for update")
    Booking findBookingForUpdate(Integer bookingId);

    @Update("""
        update booking set stay_status = 'checked_in', checked_in_at = #{now},
            operation_version = operation_version + 1, modified_time = now()
        where id = #{id} and status = 'confirmed' and stay_status = 'not_checked_in' and operation_version = #{version}
        """)
    int checkInBooking(@Param("id") Integer id, @Param("version") Long version, @Param("now") OffsetDateTime now);

    @Update("""
        update booking set status = 'completed', stay_status = 'checked_out', checked_out_at = #{now},
            operation_version = operation_version + 1, modified_time = now()
        where id = #{id} and status = 'confirmed' and stay_status = 'checked_in' and operation_version = #{version}
        """)
    int checkOutBooking(@Param("id") Integer id, @Param("version") Long version, @Param("now") OffsetDateTime now);

    @Update("update booking set admin_note = #{note}, modified_time = now() where id = #{bookingId}")
    int updateBookingNote(@Param("bookingId") Integer bookingId, @Param("note") String note);

    @Select("""
            select *
            from booking
            where upper(booking_no) = upper(#{bookingNo})
            limit 1
            """)
    Booking findBookingByBookingNo(String bookingNo);

    @Select("""
            select b.*
            from booking b
            inner join booking_guest bg on bg.booking_id = b.id
            where bg.contact_phone = #{contactPhone}
            order by b.created_time desc, b.id desc
            """)
    List<Booking> findBookingsByContactPhone(String contactPhone);

    @Select("""
            select b.* from booking b
            join line_user_binding l on l.booking_id = b.id
            where l.line_user_id = #{lineUserId} and l.status = 'active' and l.identity_verified_at is not null
            order by b.created_time desc, b.id desc
            """)
    List<Booking> findBookingsForVerifiedLineUser(String lineUserId);

    @Select("""
            select *
            from booking
            where check_in_date = #{checkInDate}
              and status <> 'cancelled'
            order by check_in_date asc, id asc
            """)
    List<Booking> findActiveBookingsByCheckInDate(LocalDate checkInDate);

    @Select("""
            select *
            from booking_guest
            where booking_id = #{bookingId}
            limit 1
            """)
    BookingGuest findBookingGuestByBookingId(Integer bookingId);

    @Select("""
            select
                bi.*,
                cu.name as camp_unit_name,
                rp.name as rate_plan_name
            from booking_item bi
            left join camp_unit cu on cu.id = bi.camp_unit_id
            left join rate_plan rp on rp.id = bi.rate_plan_id
            where bi.booking_id = #{bookingId}
            order by bi.id
            """)
    List<BookingDetailItemDto> findBookingItemsByBookingId(Integer bookingId);

    @Select("""
            select *
            from booking_timeline
            where booking_id = #{bookingId}
            order by created_time desc, id desc
            """)
    List<BookingTimeline> findBookingTimelineByBookingId(Integer bookingId);

    @Update("""
            update booking
            set status = #{status},
                operation_version = operation_version + 1,
                modified_time = now()
            where id = #{bookingId}
            """)
    void updateBookingStatus(@Param("bookingId") Integer bookingId, @Param("status") String status);

    @Update("""
            update booking
            set paid_amount = #{paidAmount},
                operation_version = operation_version + 1,
                modified_time = now()
            where id = #{bookingId}
            """)
    void updateBookingPaidAmount(@Param("bookingId") Integer bookingId, @Param("paidAmount") java.math.BigDecimal paidAmount);

    @Update("""
            update booking
            set paid_amount = #{paidAmount},
                operation_version = operation_version + 1,
                adjustment_amount = #{adjustmentAmount},
                total_amount = subtotal_amount + #{adjustmentAmount},
                admin_note = #{adminNote},
                confirm_note = #{confirmNote},
                cancel_reason = #{cancelReason},
                modified_time = now()
            where id = #{bookingId}
            """)
    void updateBookingAdminFields(
            @Param("bookingId") Integer bookingId,
            @Param("paidAmount") java.math.BigDecimal paidAmount,
            @Param("adjustmentAmount") java.math.BigDecimal adjustmentAmount,
            @Param("adminNote") String adminNote,
            @Param("confirmNote") String confirmNote,
            @Param("cancelReason") String cancelReason
    );

    @Insert("""
            insert into booking_timeline(
                booking_id,
                action_type,
                action_label,
                actor_user_id,
                actor_name,
                detail,
                created_time
            ) values (
                #{bookingId},
                #{actionType},
                #{actionLabel},
                #{actorUserId},
                #{actorName},
                #{detail},
                #{createdTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertBookingTimeline(BookingTimeline timeline);
}
