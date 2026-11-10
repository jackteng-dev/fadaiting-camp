package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.config.BookingHoldProperties;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.mapper.PaymentMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingTimeline;
import com.fadaiting.camp.service.BookingHoldService;
import com.fadaiting.camp.service.BookingInventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
public class BookingHoldServiceImpl implements BookingHoldService {
    private final BookingMapper bookings;
    private final PaymentMapper payments;
    private final BookingInventoryService inventory;
    private final BookingHoldProperties properties;
    private final Clock clock;
    private final org.springframework.transaction.support.TransactionTemplate transactions;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BookingHoldServiceImpl.class);
    public BookingHoldServiceImpl(BookingMapper bookings, PaymentMapper payments, BookingInventoryService inventory,
            BookingHoldProperties properties, Clock clock, org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.bookings = bookings; this.payments = payments; this.inventory = inventory; this.properties = properties; this.clock = clock;
        this.transactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Override public OffsetDateTime newDeadline() { return OffsetDateTime.now(clock).plusMinutes(properties.getMinutes()); }
    @Override public boolean isDue(Booking booking) {
        return "pending".equals(booking.getStatus()) && "not_checked_in".equals(booking.getStayStatus())
                && booking.getPaymentDueAt() != null && !booking.getPaymentDueAt().isAfter(OffsetDateTime.now(clock))
                && (booking.getPaidAmount() == null || booking.getPaidAmount().signum() == 0)
                && booking.getTotalAmount() != null && booking.getTotalAmount().signum() > 0;
    }
    @Override @Transactional public boolean expireBooking(Integer bookingId) {
        Booking booking = bookings.findBookingForUpdate(bookingId);
        return booking != null && expireLockedBooking(booking);
    }
    /** 僅由持有訂單 row lock 的交易呼叫。 */
    @Override @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public boolean expireLockedBooking(Booking booking) {
        if (!isDue(booking)) return false;
        BigDecimal paid = payments.sumPaidAmountByBookingId(booking.getId());
        if (paid != null && paid.signum() > 0) return false;
        OffsetDateTime now = OffsetDateTime.now(clock);
        inventory.release(booking, bookings.findBookingItemsByBookingId(booking.getId()));
        bookings.markBookingExpired(booking.getId(), now);
        invalidatePendingPayments(booking.getId());
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(booking.getId()); timeline.setActionType("booking_expired");
        timeline.setActionLabel("預約付款逾時取消"); timeline.setActorName("預約系統");
        timeline.setDetail("未於付款期限內完成付款，預約已取消；如需預約請重新查詢日期。");
        timeline.setCreatedTime(LocalDateTime.ofInstant(clock.instant(), java.time.ZoneId.systemDefault()));
        bookings.insertBookingTimeline(timeline);
        booking.setStatus("cancelled"); booking.setExpiredAt(now); booking.setOperationVersion(booking.getOperationVersion() + 1);
        return true;
    }
    @Override @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void invalidatePendingPayments(Integer bookingId) { payments.expirePendingForBooking(bookingId); }
    @Override public com.fadaiting.camp.dto.BookingExpirationResponse expireBatch() {
        var response = new com.fadaiting.camp.dto.BookingExpirationResponse();
        for (Integer id : bookings.findDueBookingIds(OffsetDateTime.now(clock), properties.getBatchSize())) {
            try {
                Boolean expired = transactions.execute(transaction -> {
                    Booking booking = bookings.findBookingForUpdateSkipLocked(id);
                    return booking != null && expireLockedBooking(booking);
                });
                if (Boolean.TRUE.equals(expired)) response.setExpiredCount(response.getExpiredCount() + 1);
                else response.setSkippedCount(response.getSkippedCount() + 1);
            } catch (Exception error) {
                response.setFailedCount(response.getFailedCount() + 1);
                LOG.error("預約 {} 逾時處理失敗，該筆已回滾；請核對庫存", id, error);
            }
        }
        return response;
    }
}
