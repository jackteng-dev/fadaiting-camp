package com.fadaiting.camp;

import com.fadaiting.camp.controller.BookingAdminController;
import com.fadaiting.camp.dto.BookingDetailDto;
import com.fadaiting.camp.pojo.*;
import com.fadaiting.camp.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentFinancialVisibilityTest {
    @Test void staffCannotReadProofThroughOrderHistory() {
        BookingService service = mock(BookingService.class);
        CampAccessService access = mock(CampAccessService.class);
        BookingAdminController controller = new BookingAdminController();
        ReflectionTestUtils.setField(controller,"bookingService",service);
        ReflectionTestUtils.setField(controller,"campAccessService",access);
        CustomUserDetails actor = mock(CustomUserDetails.class); when(actor.getId()).thenReturn(1);
        when(access.currentRole(1)).thenReturn("STAFF");
        BookingTimeline operation = new BookingTimeline(); operation.setActionType("stay_checked_in");
        BookingTimeline financial = new BookingTimeline(); financial.setActionType("payment_reconciled"); financial.setDetail("內部銀行憑證");
        BookingTimeline refund = new BookingTimeline(); refund.setActionType("refund_requested"); refund.setDetail("內部退款原因");
        PaymentRecord payment = new PaymentRecord(); payment.setReconciliationReference("內部憑證"); payment.setReconciliationNote("內部說明"); payment.setReconciledBy(1);
        BookingDetailDto detail = new BookingDetailDto(); detail.setTimeline(List.of(operation,financial,refund)); detail.setPayments(List.of(payment));
        when(service.getAdminBookingDetail(10)).thenReturn(detail);
        controller.detail(10,actor);
        assertEquals(List.of(operation),detail.getTimeline());
        assertNull(payment.getReconciliationReference()); assertNull(payment.getReconciliationNote()); assertNull(payment.getReconciledBy());
    }
}
