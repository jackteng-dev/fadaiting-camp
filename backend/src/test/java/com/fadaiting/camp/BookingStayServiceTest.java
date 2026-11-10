package com.fadaiting.camp;

import com.fadaiting.camp.dto.BookingStayUpdateRequest;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingTimeline;
import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.service.impl.BookingStayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingStayServiceTest {
    private final BookingMapper mapper = mock(BookingMapper.class);
    private final CampAccessService access = mock(CampAccessService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-05T16:30:00Z"), ZoneId.of("Asia/Taipei"));
    private final BookingStayServiceImpl service = new BookingStayServiceImpl(mapper, access, clock);
    private Booking booking;
    @BeforeEach
    void setup() {
        booking = new Booking(); booking.setId(10); booking.setStatus("confirmed");
        booking.setStayStatus("not_checked_in"); booking.setOperationVersion(7L);
        booking.setCheckInDate(LocalDate.of(2026, 10, 6)); booking.setCheckOutDate(LocalDate.of(2026, 10, 8));
        when(access.currentRole(1)).thenReturn("STAFF");
        when(mapper.findBookingForUpdate(10)).thenReturn(booking);
        when(mapper.findBookingById(10)).thenReturn(booking);
        when(mapper.checkInBooking(eq(10), eq(7L), any())).thenReturn(1);
        when(mapper.checkOutBooking(eq(10), eq(7L), any())).thenReturn(1);
    }
    private BookingStayUpdateRequest request(String action) {
        BookingStayUpdateRequest r = new BookingStayUpdateRequest(); r.setBookingId(10); r.setAction(action); r.setExpectedVersion(7L); return r;
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "STAFF"})
    void bothRolesCanRegisterArrivalAtTaipeiDateBoundary(String role) {
        when(access.currentRole(1)).thenReturn(role);
        service.updateStay(request("check_in"), 1, "現場");
        verify(mapper).checkInBooking(10, 7L, OffsetDateTime.now(clock));
        verify(mapper).insertBookingTimeline(argThat(t -> "checked_in".equals(t.getActionType()) && t.getActorUserId() == 1));
    }
    @Test void futureArrivalIsRejected() {
        booking.setCheckInDate(LocalDate.of(2026, 10, 7));
        assertThrows(BookingConflictException.class, () -> service.updateStay(request("check_in"), 1, "現場"));
        verify(mapper, never()).checkInBooking(any(), any(), any());
    }
    @Test void arrivalOnDepartureDateIsRejected() {
        booking.setCheckOutDate(LocalDate.of(2026, 10, 6));
        assertThrows(BookingConflictException.class, () -> service.updateStay(request("check_in"), 1, "現場"));
    }
    @Test void lateArrivalBeforeDepartureIsAllowed() {
        booking.setCheckInDate(LocalDate.of(2026, 10, 5));
        service.updateStay(request("check_in"), 1, "現場");
        verify(mapper).checkInBooking(eq(10), eq(7L), any());
    }
    @ParameterizedTest @ValueSource(strings = {"pending", "cancelled", "completed"})
    void nonConfirmedBookingsCannotCheckIn(String status) {
        booking.setStatus(status);
        assertThrows(BookingConflictException.class, () -> service.updateStay(request("check_in"), 1, "現場"));
        verify(mapper, never()).checkInBooking(any(), any(), any());
    }
    @Test void cannotCheckOutWithoutArrival() {
        assertThrows(BookingConflictException.class, () -> service.updateStay(request("check_out"), 1, "現場"));
        verify(mapper, never()).checkOutBooking(any(), any(), any());
    }
    @Test void earlyCheckoutUsesRecordedArrivalAndWritesActorHistory() {
        booking.setStayStatus("checked_in"); booking.setCheckedInAt(OffsetDateTime.now(clock).minusMinutes(10));
        service.updateStay(request("check_out"), 1, "現場");
        verify(mapper).checkOutBooking(10, 7L, OffsetDateTime.now(clock));
        verify(mapper).insertBookingTimeline(argThat(t -> "checked_out".equals(t.getActionType()) && "現場".equals(t.getActorName())));
        verify(mapper, never()).updateBookingStatus(any(), any());
    }
    @Test void staleVersionCannotMutateOrWriteHistory() {
        BookingStayUpdateRequest r = request("check_in"); r.setExpectedVersion(6L);
        assertThrows(BookingConflictException.class, () -> service.updateStay(r, 1, "現場"));
        verify(mapper, never()).checkInBooking(any(), any(), any()); verify(mapper, never()).insertBookingTimeline(any());
    }
    @Test void alreadyArrivedCannotBeRegisteredTwice() {
        booking.setStayStatus("checked_in");
        assertThrows(BookingConflictException.class, () -> service.updateStay(request("check_in"), 1, "現場"));
    }
    @Test void actorIsRecheckedAfterWaitingForRowLock() {
        when(access.currentRole(1)).thenReturn("STAFF", "");
        assertThrows(AccessDeniedException.class, () -> service.updateStay(request("check_in"), 1, "現場"));
        verify(mapper, never()).checkInBooking(any(), any(), any());
    }
    @Test void unauthorizedActorCannotReadBooking() {
        when(access.currentRole(1)).thenReturn("");
        assertThrows(AccessDeniedException.class, () -> service.updateStay(request("check_in"), 1, "現場"));
        verifyNoInteractions(mapper);
    }
    @Test void missingBookingIsRejected() {
        when(mapper.findBookingForUpdate(10)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.updateStay(request("check_in"), 1, "現場"));
    }
    @Test void conditionalUpdateFailureWritesNoHistory() {
        when(mapper.checkInBooking(any(), any(), any())).thenReturn(0);
        assertThrows(BookingConflictException.class, () -> service.updateStay(request("check_in"), 1, "現場"));
        verify(mapper, never()).insertBookingTimeline(any());
    }
    @Test void checkoutCannotPrecedeRecordedArrival() {
        booking.setStayStatus("checked_in"); booking.setCheckedInAt(OffsetDateTime.now(clock).plusMinutes(1));
        assertThrows(BookingConflictException.class, () -> service.updateStay(request("check_out"), 1, "現場"));
    }
    @Test void unknownActionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.updateStay(request("delete"), 1, "現場"));
    }
}
