package com.fadaiting.camp;

import com.fadaiting.camp.dto.BookingDetailItemDto;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.mapper.*;
import com.fadaiting.camp.pojo.*;
import com.fadaiting.camp.service.impl.BookingInventoryServiceImpl;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingInventoryServiceTest {
    private final AvailabilityMapper availability = mock(AvailabilityMapper.class);
    private final BookingMapper bookings = mock(BookingMapper.class);
    private final RentalItemMapper rentals = mock(RentalItemMapper.class);
    private final BookingInventoryServiceImpl service = new BookingInventoryServiceImpl(availability, bookings, rentals);
    private final LocalDate start = LocalDate.of(2026,10,6), end = start.plusDays(2);
    private Booking booking() { Booking b = new Booking(); b.setCampSiteId(1); b.setCheckInDate(start); b.setCheckOutDate(end); return b; }
    private BookingDetailItemDto item(int unit, int quantity, Integer rental) {
        BookingDetailItemDto item = new BookingDetailItemDto(); item.setCampUnitId(unit); item.setQuantity(quantity); item.setRentalItemId(rental); return item;
    }
    @Test void releaseAggregatesMultipleUnitsAndExcludesRentalRows() {
        when(availability.releaseReserved(any(), any(), anyInt())).thenReturn(1);
        service.release(booking(), List.of(item(2, 2, null), item(1, 3, null), item(2, 1, null), item(2, 5, 9)));
        var order = inOrder(availability);
        order.verify(availability).releaseReserved(1, start, 3); order.verify(availability).releaseReserved(1, start.plusDays(1), 3);
        order.verify(availability).releaseReserved(2, start, 3); order.verify(availability).releaseReserved(2, start.plusDays(1), 3);
        verifyNoMoreInteractions(availability);
    }
    @Test void inventoryUnderflowIsAnErrorRatherThanSilentClamp() {
        when(availability.releaseReserved(1, start, 2)).thenReturn(0);
        assertThrows(BookingConflictException.class, () -> service.release(booking(), List.of(item(1,2,null))));
    }
    @Test void conditionalReservationRejectsConcurrentSellout() {
        CampUnit u = new CampUnit(); u.setId(1); u.setCampSiteId(1); u.setQuantity(1);
        when(availability.reserveIfAvailable(1,1,start,1,1)).thenReturn(0);
        assertThrows(BookingConflictException.class, () -> service.reserve(u,start,end,1));
    }
    @Test void reopeningRequiresRentalCapacityBeforeAnySiteReservation() {
        RentalItem rental = new RentalItem(); rental.setId(9); rental.setIsActive(true); rental.setCampSiteId(1); rental.setTotalQuantity(2);
        when(rentals.findForUpdate(9)).thenReturn(rental); when(rentals.findById(9)).thenReturn(rental);
        when(rentals.findMaxReservedQuantityByRentalItemAndDateRange(9,start,end)).thenReturn(2);
        assertThrows(BookingConflictException.class, () -> service.reopen(booking(),List.of(item(1,1,null),item(1,1,9))));
        verifyNoInteractions(availability); verifyNoInteractions(bookings);
    }
    @Test void cancellingDoesNotRequireAnActiveUnit() {
        when(availability.releaseReserved(any(),any(),anyInt())).thenReturn(1);
        service.release(booking(),List.of(item(1,1,null))); verifyNoInteractions(bookings);
    }
}
