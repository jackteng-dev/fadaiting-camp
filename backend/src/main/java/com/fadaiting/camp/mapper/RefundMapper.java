package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.RefundRecord;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface RefundMapper {
    @Select("""
            select count(*) from refund_record r join payment_record p on p.id = r.payment_record_id
            where p.booking_id = #{bookingId} and r.status in ('requested','approved','completed')
            """)
    int countBlockingForBooking(Integer bookingId);
    @Select("select count(*) from refund_record r join payment_record p on p.id = r.payment_record_id where p.booking_id = #{bookingId} and r.status = 'completed'")
    int countCompletedForBooking(Integer bookingId);
    @Select("select * from refund_record where payment_record_id = #{id} order by id desc")
    List<RefundRecord> findByPayment(Integer id);
    @Select("select * from refund_record where id = #{id}")
    RefundRecord findById(Integer id);
    @Select("select * from refund_record where idempotency_key = #{key}")
    RefundRecord findByKey(UUID key);
    @Select("select coalesce(sum(amount),0) from refund_record where payment_record_id = #{id} and status in ('requested','approved','completed')")
    BigDecimal sumCommitted(Integer id);
    @Select("select coalesce(sum(amount),0) from refund_record where payment_record_id = #{id} and status = 'completed'")
    BigDecimal sumCompleted(Integer id);
    @Select("select count(*) from refund_record where payment_record_id = #{paymentId} and external_reference = #{reference}")
    int countReference(@Param("paymentId") Integer paymentId, @Param("reference") String reference);
    @Insert("""
            insert into refund_record(payment_record_id,amount,status,idempotency_key,reason,requested_by,requested_at)
            values(#{paymentRecordId},#{amount},'requested',#{idempotencyKey},#{reason},#{requestedBy},#{requestedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RefundRecord record);
    @Update("""
            update refund_record set status = #{status}, decision_note = #{decisionNote}, external_reference = #{externalReference},
                decided_by = #{decidedBy}, decided_at = #{decidedAt}, completed_by = #{completedBy}, completed_at = #{completedAt},
                operation_version = operation_version + 1 where id = #{id}
            """)
    void update(RefundRecord record);
}
