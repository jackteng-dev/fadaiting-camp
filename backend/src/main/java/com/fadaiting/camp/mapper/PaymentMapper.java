package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.PaymentRecord;
import com.fadaiting.camp.pojo.PaymentMethodConfig;
import com.fadaiting.camp.dto.PaymentAdminRowDto;
import com.fadaiting.camp.dto.PaymentAdminSummaryDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentMapper {
    @Update("update payment_record set status = 'expired', operation_version = operation_version + 1, modified_time = now() where booking_id = #{bookingId} and status = 'pending'")
    int expirePendingForBooking(Integer bookingId);

    @Select("""
            select *
            from payment_method_config
            order by sort_order asc, id asc
            """)
    List<PaymentMethodConfig> findPaymentMethodConfigs();

    @Select("""
            select *
            from payment_method_config
            where is_active = true
            order by sort_order asc, id asc
            """)
    List<PaymentMethodConfig> findActivePaymentMethodConfigs();

    @Select("""
            select *
            from payment_method_config
            where provider = #{provider}
            limit 1
            """)
    PaymentMethodConfig findPaymentMethodConfigByProvider(String provider);

    @Update("""
            update payment_method_config
            set display_name = #{displayName},
                description = #{description},
                payment_method = #{paymentMethod},
                sort_order = #{sortOrder},
                is_active = #{isActive},
                modified_time = now()
            where provider = #{provider}
            """)
    void updatePaymentMethodConfig(PaymentMethodConfig config);

    @Insert("""
            insert into payment_record(
                booking_id,
                provider,
                payment_method,
                status,
                amount,
                currency,
                merchant_order_no,
                provider_transaction_id,
                payment_no,
                payment_url,
                callback_payload,
                paid_time,
                created_time,
                modified_time
            ) values (
                #{bookingId},
                #{provider},
                #{paymentMethod},
                #{status},
                #{amount},
                #{currency},
                #{merchantOrderNo},
                #{providerTransactionId},
                #{paymentNo},
                #{paymentUrl},
                #{callbackPayload},
                #{paidTime},
                #{createdTime},
                #{modifiedTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPaymentRecord(PaymentRecord record);

    @Select("""
            select p.*, coalesce((select sum(r.amount) from refund_record r where r.payment_record_id = p.id and r.status = 'completed'),0) as refunded_amount
            from payment_record p
            where booking_id = #{bookingId}
            order by created_time desc, id desc
            """)
    List<PaymentRecord> findByBookingId(Integer bookingId);

    @Select("""
            <script>
            select
                pr.*,
                b.booking_no,
                b.status as booking_status,
                bg.contact_name,
                bg.contact_phone
            from payment_record pr
            left join booking b on b.id = pr.booking_id
            left join booking_guest bg on bg.booking_id = b.id
            where 1 = 1
            <if test="bookingNo != null and bookingNo != ''">
                and b.booking_no like concat('%', #{bookingNo}, '%')
            </if>
            <if test="provider != null and provider != ''">
                and pr.provider = #{provider}
            </if>
            <if test="status != null and status != ''">
                and pr.status = #{status}
            </if>
            <if test="merchantOrderNo != null and merchantOrderNo != ''">
                and pr.merchant_order_no like concat('%', #{merchantOrderNo}, '%')
            </if>
            <if test="startDate != null and startDate != ''">
                and pr.created_time <![CDATA[>=]]> cast(#{startDate} as date)
            </if>
            <if test="endDate != null and endDate != ''">
                and pr.created_time <![CDATA[<]]> cast(#{endDate} as date) + interval '1 day'
            </if>
            order by pr.created_time desc, pr.id desc
            </script>
            """)
    List<PaymentAdminRowDto> findAdminPayments(
            @Param("bookingNo") String bookingNo,
            @Param("provider") String provider,
            @Param("status") String status,
            @Param("merchantOrderNo") String merchantOrderNo,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    @Select("""
            <script>
            select
                pr.status,
                count(*) as record_count,
                coalesce(sum(pr.amount), 0) as total_amount
            from payment_record pr
            left join booking b on b.id = pr.booking_id
            left join booking_guest bg on bg.booking_id = b.id
            where 1 = 1
            <if test="bookingNo != null and bookingNo != ''">
                and b.booking_no like concat('%', #{bookingNo}, '%')
            </if>
            <if test="provider != null and provider != ''">
                and pr.provider = #{provider}
            </if>
            <if test="status != null and status != ''">
                and pr.status = #{status}
            </if>
            <if test="merchantOrderNo != null and merchantOrderNo != ''">
                and pr.merchant_order_no like concat('%', #{merchantOrderNo}, '%')
            </if>
            <if test="startDate != null and startDate != ''">
                and pr.created_time <![CDATA[>=]]> cast(#{startDate} as date)
            </if>
            <if test="endDate != null and endDate != ''">
                and pr.created_time <![CDATA[<]]> cast(#{endDate} as date) + interval '1 day'
            </if>
            group by pr.status
            order by pr.status
            </script>
            """)
    List<PaymentAdminSummaryDto> findAdminPaymentSummary(
            @Param("bookingNo") String bookingNo,
            @Param("provider") String provider,
            @Param("status") String status,
            @Param("merchantOrderNo") String merchantOrderNo,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    @Select("""
            select *
            from payment_record
            where booking_id = #{bookingId}
              and status = 'pending'
            order by created_time desc, id desc
            """)
    List<PaymentRecord> findPendingByBookingId(Integer bookingId);

    @Select("""
            select *
            from payment_record
            where booking_id = #{bookingId}
              and provider = #{provider}
              and payment_method = #{paymentMethod}
              and status = 'pending'
            order by created_time desc, id desc
            limit 1
            """)
    PaymentRecord findLatestPendingByBookingIdAndProvider(
            @Param("bookingId") Integer bookingId,
            @Param("provider") String provider,
            @Param("paymentMethod") String paymentMethod
    );

    @Select("""
            <script>
            select *
            from payment_record
            where status = 'pending'
              and created_time &lt; #{createdBefore}
            <if test="bookingId != null">
              and booking_id = #{bookingId}
            </if>
            order by created_time asc, id asc
            </script>
            """)
    List<PaymentRecord> findStalePendingPayments(
            @Param("bookingId") Integer bookingId,
            @Param("createdBefore") LocalDateTime createdBefore
    );

    @Select("""
            select *
            from payment_record
            where id = #{id}
            limit 1
            """)
    PaymentRecord findById(Integer id);

    @Select("""
            select *
            from payment_record
            where merchant_order_no = #{merchantOrderNo}
            limit 1
            """)
    PaymentRecord findByMerchantOrderNo(String merchantOrderNo);

    @Select("""
            select *
            from payment_record
            where provider_transaction_id = #{providerTransactionId}
            order by id desc
            limit 1
            """)
    PaymentRecord findByProviderTransactionId(String providerTransactionId);

    @Select("""
            select coalesce(sum(p.amount - coalesce(r.refunded, 0)), 0)
            from payment_record p
            left join (select payment_record_id, sum(amount) as refunded from refund_record where status = 'completed' group by payment_record_id) r
                on r.payment_record_id = p.id
            where p.booking_id = #{bookingId} and p.status = 'paid'
            """)
    BigDecimal sumPaidAmountByBookingId(Integer bookingId);

    @Update("""
            update payment_record
            set status = #{status},
                operation_version = operation_version + 1,
                provider_transaction_id = #{providerTransactionId},
                payment_no = #{paymentNo},
                payment_url = #{paymentUrl},
                callback_payload = #{callbackPayload},
                paid_time = #{paidTime},
                modified_time = now()
            where id = #{id}
            """)
    void updatePaymentRecord(PaymentRecord record);

    @Update("""
            update payment_record
            set status = #{status},
                operation_version = operation_version + 1,
                callback_payload = #{callbackPayload},
                paid_time = #{paidTime},
                modified_time = now()
            where id = #{paymentRecordId}
            """)
    void updatePaymentStatus(
            @Param("paymentRecordId") Integer paymentRecordId,
            @Param("status") String status,
            @Param("callbackPayload") String callbackPayload,
            @Param("paidTime") java.time.LocalDateTime paidTime
    );

    @Update("update payment_record set operation_version = operation_version + 1, modified_time = now() where id = #{id}")
    void rotateFinancialVersion(Integer id);

    @Update("""
            update payment_record set status = #{status}, reconciliation_status = #{reconciliationStatus},
                reconciliation_note = #{reconciliationNote}, reconciliation_reference = #{reconciliationReference},
                reconciled_by = #{reconciledBy}, reconciled_at = #{reconciledAt}, operation_version = operation_version + 1,
                modified_time = now() where id = #{id}
            """)
    void updateReconciliation(PaymentRecord record);
}
