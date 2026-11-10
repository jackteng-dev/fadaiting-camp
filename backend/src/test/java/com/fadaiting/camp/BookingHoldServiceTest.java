package com.fadaiting.camp;

import com.fadaiting.camp.config.BookingHoldProperties;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.mapper.PaymentMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.service.BookingInventoryService;
import com.fadaiting.camp.service.impl.BookingHoldServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingHoldServiceTest {
    private final BookingMapper bookings = mock(BookingMapper.class);
    private final PaymentMapper payments = mock(PaymentMapper.class);
    private final BookingInventoryService inventory = mock(BookingInventoryService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-05T16:30:00Z"), ZoneId.of("Asia/Taipei"));
    private final BookingHoldServiceImpl service = new BookingHoldServiceImpl(bookings, payments, inventory,
            new BookingHoldProperties(), clock, mock(PlatformTransactionManager.class));
    private Booking booking;
    @BeforeEach void setup() {
        booking = new Booking(); booking.setId(10); booking.setStatus("pending"); booking.setStayStatus("not_checked_in");
        booking.setOperationVersion(2L); booking.setPaidAmount(BigDecimal.ZERO); booking.setTotalAmount(BigDecimal.TEN);
        booking.setPaymentDueAt(OffsetDateTime.now(clock));
        when(bookings.findBookingForUpdate(10)).thenReturn(booking);
        when(bookings.findBookingItemsByBookingId(10)).thenReturn(List.of());
        when(payments.sumPaidAmountByBookingId(10)).thenReturn(BigDecimal.ZERO);
    }
    @Test void expiresAtExactDeadlineWithOneHistoryAndPaymentInvalidation() {
        assertTrue(service.expireBooking(10)); assertEquals("cancelled", booking.getStatus()); assertEquals(3L, booking.getOperationVersion());
        verify(inventory).release(booking, List.of()); verify(payments).expirePendingForBooking(10);
        verify(bookings).insertBookingTimeline(argThat(t -> "booking_expired".equals(t.getActionType()) && t.getActorUserId() == null));
        assertFalse(service.expireBooking(10)); verify(bookings, times(1)).markBookingExpired(10, OffsetDateTime.now(clock));
    }
    @Test void oldBookingsWithoutDeadlineArePreserved() { booking.setPaymentDueAt(null); assertFalse(service.expireBooking(10)); verifyNoInteractions(inventory); }
    @Test void futureDeadlineIsPreserved() { booking.setPaymentDueAt(OffsetDateTime.now(clock).plusNanos(1)); assertFalse(service.expireBooking(10)); }
    @Test void partialPaymentIsPreserved() { booking.setPaidAmount(BigDecimal.ONE); assertFalse(service.expireBooking(10)); }
    @Test void paidLedgerIsPreservedEvenIfLegacyAggregateIsStale() { when(payments.sumPaidAmountByBookingId(10)).thenReturn(BigDecimal.ONE); assertFalse(service.expireBooking(10)); verifyNoInteractions(inventory); }
    @Test void zeroTotalDoesNotRequirePayment() { booking.setTotalAmount(BigDecimal.ZERO); assertFalse(service.expireBooking(10)); }
    @Test void confirmedCompletedCancelledAndActualStaysArePreserved() {
        for (String status : List.of("confirmed", "completed", "cancelled")) { booking.setStatus(status); assertFalse(service.isDue(booking)); }
        booking.setStatus("pending"); booking.setStayStatus("checked_in"); assertFalse(service.isDue(booking));
    }
    @Test void brokenInventoryStopsBeforeStatusOrPaymentMutation() {
        doThrow(new IllegalStateException("合成故障")).when(inventory).release(any(), any());
        assertThrows(IllegalStateException.class, () -> service.expireBooking(10));
        verify(bookings, never()).markBookingExpired(any(), any()); verify(payments, never()).expirePendingForBooking(any());
    }
    @Test void newDeadlineUsesTaipeiClockAndThirtyMinutes() { assertEquals("2026-10-06T01:00+08:00", service.newDeadline().toString()); }
    @Test void workerSkipsLockedBookingWithoutMutation() {
        when(bookings.findDueBookingIds(any(), anyInt())).thenReturn(List.of(10)); when(bookings.findBookingForUpdateSkipLocked(10)).thenReturn(null);
        assertEquals(1, service.expireBatch().getSkippedCount()); verifyNoInteractions(inventory);
    }
}
