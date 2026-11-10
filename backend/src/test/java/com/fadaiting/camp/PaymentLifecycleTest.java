package com.fadaiting.camp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.config.*;
import com.fadaiting.camp.dto.PublicPaymentConfirmRequest;
import com.fadaiting.camp.mapper.*;
import com.fadaiting.camp.pojo.*;
import com.fadaiting.camp.service.*;
import com.fadaiting.camp.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentLifecycleTest {
    private final BookingMapper bookings = mock(BookingMapper.class);
    private final PaymentMapper payments = mock(PaymentMapper.class);
    private final BookingHoldService holds = mock(BookingHoldService.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl();
    private final EcpayProperties ecpay = new EcpayProperties();
    private Booking booking; private PaymentRecord record;
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"bookingMapper",bookings); ReflectionTestUtils.setField(service,"paymentMapper",payments);
        ReflectionTestUtils.setField(service,"bookingHoldService",holds); ReflectionTestUtils.setField(service,"objectMapper",new ObjectMapper());
        ReflectionTestUtils.setField(service,"ecpayProperties",ecpay); ReflectionTestUtils.setField(service,"linePayProperties",new LinePayProperties());
        ReflectionTestUtils.setField(service,"notificationService",mock(NotificationService.class));
        booking = new Booking(); booking.setId(10); booking.setStatus("pending"); booking.setPaidAmount(BigDecimal.ZERO); booking.setTotalAmount(new BigDecimal("1000"));
        record = new PaymentRecord(); record.setId(20); record.setBookingId(10); record.setProvider("ecpay"); record.setStatus("pending"); record.setAmount(new BigDecimal("1000")); record.setCreatedTime(LocalDateTime.now());
        when(bookings.findBookingForUpdate(10)).thenReturn(booking); when(bookings.findBookingById(10)).thenReturn(booking);
        when(payments.findById(20)).thenReturn(record); when(payments.findByMerchantOrderNo("CLOUD_TEST")).thenReturn(record);
        when(payments.findStalePendingPayments(any(),any())).thenReturn(List.of()); when(payments.sumPaidAmountByBookingId(10)).thenReturn(record.getAmount());
    }
    private PublicPaymentConfirmRequest request() { PublicPaymentConfirmRequest r = new PublicPaymentConfirmRequest(); r.setPaymentRecordId(20); return r; }
    @Test void repeatedMockConfirmOnlyCreditsAndQueuesOnce() {
        service.confirmPublicPayment(request()); service.confirmPublicPayment(request());
        assertEquals("paid",record.getStatus()); verify(bookings,times(1)).updateBookingPaidAmount(10,new BigDecimal("1000")); verify(bookings,times(1)).insertBookingTimeline(any());
    }
    @Test void cancelledBookingCannotBePaidThroughMockConfirm() {
        booking.setStatus("cancelled"); service.confirmPublicPayment(request()); assertEquals("pending",record.getStatus()); verify(payments,never()).updatePaymentRecord(any());
    }
    @Test void expiredPaymentCannotBePaidThroughMockConfirm() {
        record.setStatus("expired"); service.confirmPublicPayment(request()); verify(bookings,never()).updateBookingPaidAmount(any(),any());
    }
    @Test void changingRemainingAmountInvalidatesOldMockPayment() {
        booking.setPaidAmount(new BigDecimal("100")); service.confirmPublicPayment(request()); assertEquals("expired",record.getStatus()); verify(bookings,never()).updateBookingPaidAmount(any(),any());
    }
    @Test void unsignedBrowserReturnNeverMutatesPaymentEvenWhenEnabled() {
        configure(); service.resolveEcpayReturnRedirectUrl(Map.of("MerchantTradeNo","CLOUD_TEST","RtnCode","1","TradeAmt","1000")); verifyNoInteractions(payments);
    }
    @Test void badSignatureCannotMutatePayment() {
        configure(); assertThrows(IllegalArgumentException.class, () -> service.handleEcpayCallback(Map.of("CheckMacValue","BAD"))); verifyNoInteractions(payments);
    }
    private Map<String,String> signedSuccess() {
        return Map.of("MerchantID","TEST","MerchantTradeNo","CLOUD_TEST","RtnCode","1","TradeAmt","1000",
                "CheckMacValue","FF834730DD8E54A30DD0EAAA0D928CB4D4CD0B1962B70BB1ECB4E49AACF3C2A3");
    }
    @Test void signedLatePaymentIsRecordedForReviewWithoutReopeningOrCrediting() {
        configure(); booking.setStatus("cancelled"); record.setStatus("expired");
        assertEquals("1|OK",service.handleEcpayCallback(signedSuccess())); assertEquals("paid_review",record.getStatus());
        verify(bookings,never()).updateBookingPaidAmount(any(),any()); verify(bookings,never()).updateBookingStatus(any(),any());
        verify(bookings).insertBookingTimeline(argThat(t -> "payment_review_required".equals(t.getActionType())));
        service.handleEcpayCallback(signedSuccess()); verify(payments,times(1)).updatePaymentRecord(any());
    }
    @Test void validSignedCallbackCreditsExactlyOnce() {
        configure(); service.handleEcpayCallback(signedSuccess()); service.handleEcpayCallback(signedSuccess());
        assertEquals("paid",record.getStatus()); verify(bookings,times(1)).updateBookingPaidAmount(any(),any()); verify(bookings,times(1)).insertBookingTimeline(any());
    }
    @Test void validSignedBrowserReturnOnlyRedirectsAndNeverCredits() {
        configure(); assertTrue(service.resolveEcpayReturnRedirectUrl(signedSuccess()).contains("/payment/confirm"));
        assertEquals("pending",record.getStatus()); verify(payments,never()).updatePaymentRecord(any()); verify(bookings,never()).updateBookingPaidAmount(any(),any());
    }
    @Test void validSignatureDoesNotPermitAmountMismatch() {
        configure(); Map<String,String> fields = new HashMap<>(signedSuccess()); fields.put("TradeAmt","999");
        fields.put("CheckMacValue","0E61474877D1C88AFDCD2E5304283E973F37E030F4DC379E7AABCB559420B1B5");
        assertThrows(IllegalArgumentException.class, () -> service.handleEcpayCallback(fields)); verify(payments,never()).updatePaymentRecord(any());
    }
    @Test void missingAmountIsRejectedEvenWithValidSignature() {
        configure(); Map<String,String> fields = new HashMap<>(signedSuccess()); fields.remove("TradeAmt");
        fields.put("CheckMacValue","1A151D63E090ABBC7B983C3825C25AD0C9AC2CA963D0CBA840898319E300A7A4");
        assertThrows(IllegalArgumentException.class, () -> service.handleEcpayCallback(fields)); verify(payments,never()).updatePaymentRecord(any());
    }
    private void configure() {
        ecpay.setEnabled(true); ecpay.setMerchantId("TEST"); ecpay.setHashKey("CLOUD_TEST_KEY"); ecpay.setHashIv("CLOUD_TEST_IV");
        ecpay.setCheckoutUrl("https://example.invalid/checkout"); ecpay.setReturnUrl("https://example.invalid/callback");
    }
}
