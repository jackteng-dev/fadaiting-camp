package com.fadaiting.camp;

import com.fadaiting.camp.dto.BookingAdminUpdateRequest;
import com.fadaiting.camp.dto.BookingStatusUpdateRequest;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingStaffAccessTest {
    private final BookingMapper mapper = mock(BookingMapper.class);
    private final CampAccessService access = mock(CampAccessService.class);
    private final BookingServiceImpl service = new BookingServiceImpl();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "bookingMapper", mapper);
        ReflectionTestUtils.setField(service, "campAccessService", access);
        when(access.currentRole(1)).thenReturn("STAFF");
    }

    private BookingStatusUpdateRequest status(String status) {
        BookingStatusUpdateRequest request = new BookingStatusUpdateRequest();
        request.setBookingId(10);
        request.setStatus(status);
        return request;
    }

    @Test
    void staffCannotCancelConfirmOrReopenBookings() {
        for (String status : new String[]{"cancelled", "confirmed", "pending", "completed"}) {
            assertThrows(AccessDeniedException.class, () -> service.updateBookingStatus(status(status), 1, "現場"));
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void amountMutationFailsBeforeReadingOrWritingOrder() {
        assertThrows(AccessDeniedException.class, () -> service.updateAdminBooking(new BookingAdminUpdateRequest(), 1, "現場"));
        verifyNoInteractions(mapper);
    }
}
