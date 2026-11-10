package com.fadaiting.camp;

import com.fadaiting.camp.dto.BookingStatusUpdateRequest;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.service.impl.BookingServiceImpl;
import com.fadaiting.camp.utils.BookingStatePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingStateTransitionTest {
    private final BookingMapper mapper = mock(BookingMapper.class);
    private final CampAccessService access = mock(CampAccessService.class);
    private final BookingServiceImpl service = new BookingServiceImpl();
    private Booking booking;
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "bookingMapper", mapper); ReflectionTestUtils.setField(service, "campAccessService", access);
        ReflectionTestUtils.setField(service, "bookingHoldService", mock(com.fadaiting.camp.service.BookingHoldService.class));
        when(access.currentRole(1)).thenReturn("ADMIN");
        booking = new Booking(); booking.setId(10); booking.setStatus("pending"); booking.setStayStatus("not_checked_in"); booking.setOperationVersion(7L);
        when(mapper.findBookingForUpdate(10)).thenReturn(booking); when(mapper.findBookingItemsByBookingId(10)).thenReturn(List.of());
    }
    private BookingStatusUpdateRequest request(String status) {
        BookingStatusUpdateRequest r = new BookingStatusUpdateRequest(); r.setBookingId(10); r.setStatus(status); r.setExpectedVersion(7L); return r;
    }
    @Test void pendingCanBeConfirmedWithHistory() {
        service.updateBookingStatus(request("confirmed"), 1, "管理員");
        verify(mapper).updateBookingStatus(10, "confirmed"); verify(mapper).insertBookingTimeline(any());
    }
    @Test void manualCompletionCannotBypassStayRegistration() {
        booking.setStatus("confirmed"); assertThrows(BookingConflictException.class, () -> service.updateBookingStatus(request("completed"), 1, "管理員"));
        verify(mapper, never()).updateBookingStatus(any(), any());
    }
    @Test void checkedInBookingCannotBeCancelled() {
        booking.setStatus("confirmed"); booking.setStayStatus("checked_in");
        assertThrows(BookingConflictException.class, () -> service.updateBookingStatus(request("cancelled"), 1, "管理員"));
    }
    @Test void completedLegacyBookingIsTerminal() {
        booking.setStatus("completed"); assertThrows(BookingConflictException.class, () -> service.updateBookingStatus(request("confirmed"), 1, "管理員"));
    }
    @Test void stalePageCannotOverwriteCancelledAndReopenedBooking() {
        BookingStatusUpdateRequest r = request("confirmed"); r.setExpectedVersion(6L);
        assertThrows(BookingConflictException.class, () -> service.updateBookingStatus(r, 1, "管理員"));
        verify(mapper, never()).updateBookingStatus(any(), any());
    }
    @Test void noChangeDoesNotRotateVersionOrAppendHistory() {
        service.updateBookingStatus(request("pending"), 1, "管理員"); verify(mapper, never()).updateBookingStatus(any(), any()); verify(mapper, never()).insertBookingTimeline(any());
    }
    @Test void actorRevokedWhileWaitingCannotChangeBooking() {
        when(access.currentRole(1)).thenReturn("ADMIN", "STAFF");
        assertThrows(AccessDeniedException.class, () -> service.updateBookingStatus(request("confirmed"), 1, "管理員")); verify(mapper, never()).updateBookingStatus(any(), any());
    }
    @Test void policyAllowsOnlyDocumentedTransitions() {
        assertTrue(BookingStatePolicy.allows("cancelled", "not_checked_in", "pending"));
        assertTrue(BookingStatePolicy.allows("cancelled", "not_checked_in", "confirmed"));
        assertFalse(BookingStatePolicy.allows("confirmed", "not_checked_in", "pending"));
        assertFalse(BookingStatePolicy.allows("confirmed", "checked_in", "cancelled"));
        assertFalse(BookingStatePolicy.allows("completed", "checked_out", "confirmed"));
    }
}
