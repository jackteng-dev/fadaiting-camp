package com.fadaiting.camp.service;

import com.fadaiting.camp.pojo.Booking;
import java.time.OffsetDateTime;

public interface BookingHoldService {
    OffsetDateTime newDeadline();
    boolean isDue(Booking booking);
    boolean expireBooking(Integer bookingId);
    boolean expireLockedBooking(Booking booking);
    void invalidatePendingPayments(Integer bookingId);
    com.fadaiting.camp.dto.BookingExpirationResponse expireBatch();
}
