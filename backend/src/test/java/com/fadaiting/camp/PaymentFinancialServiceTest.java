package com.fadaiting.camp;

import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.mapper.*;
import com.fadaiting.camp.pojo.*;
import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.service.impl.PaymentFinancialServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentFinancialServiceTest {
    private final BookingMapper bookings = mock(BookingMapper.class);
    private final PaymentMapper payments = mock(PaymentMapper.class);
    private final RefundMapper refunds = mock(RefundMapper.class);
    private final CampAccessService access = mock(CampAccessService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T01:00:00Z"),ZoneId.of("Asia/Taipei"));
    private final PaymentFinancialServiceImpl service = new PaymentFinancialServiceImpl(bookings,payments,refunds,access,clock);
    private Booking booking; private PaymentRecord payment; private RefundRecord refund;
    private final UUID key = UUID.fromString("c890ea1e-5d76-4c0e-8252-2e0b3191812e");
    @BeforeEach void setup() {
        when(access.currentRole(1)).thenReturn("ADMIN");
        booking = new Booking(); booking.setId(10); booking.setStatus("cancelled"); booking.setPaidAmount(new BigDecimal("1000")); booking.setOperationVersion(8L);
        payment = new PaymentRecord(); payment.setId(20); payment.setBookingId(10); payment.setStatus("paid"); payment.setCurrency("TWD"); payment.setAmount(new BigDecimal("1000")); payment.setOperationVersion(3L); payment.setReconciliationStatus("unreviewed");
        refund = new RefundRecord(); refund.setId(30); refund.setPaymentRecordId(20); refund.setAmount(new BigDecimal("200")); refund.setOperationVersion(0L); refund.setStatus("requested"); refund.setIdempotencyKey(key); refund.setReason("合成原因");
        when(payments.findById(20)).thenReturn(payment); when(bookings.findBookingForUpdate(10)).thenReturn(booking);
        when(refunds.findById(30)).thenReturn(refund); when(refunds.sumCompleted(20)).thenReturn(BigDecimal.ZERO); when(refunds.sumCommitted(20)).thenReturn(BigDecimal.ZERO); when(refunds.findByPayment(20)).thenReturn(List.of());
        when(payments.sumPaidAmountByBookingId(10)).thenReturn(new BigDecimal("1000"));
        doAnswer(invocation -> { ((RefundRecord)invocation.getArgument(0)).setId(30); return null; }).when(refunds).insert(any());
    }
    private RefundCreateRequest create() { RefundCreateRequest request = new RefundCreateRequest(); request.setPaymentRecordId(20); request.setAmount(new BigDecimal("200")); request.setReason("合成原因"); request.setExpectedVersion(3L); request.setIdempotencyKey(key); return request; }
    private RefundActionRequest action(String action) { RefundActionRequest request = new RefundActionRequest(); request.setAction(action); request.setExpectedVersion(0L); request.setNote("合成處理"); request.setExternalReference("CLOUD-PROOF"); return request; }
    private PaymentReconciliationRequest reconcile(String resolution) { PaymentReconciliationRequest request = new PaymentReconciliationRequest(); request.setResolution(resolution); request.setExpectedVersion(3L); request.setExpectedBookingVersion(8L); request.setNote("合成查單"); request.setExternalReference("CLOUD-QUERY"); return request; }
    @Test void staffCannotReadFinancialData() { when(access.currentRole(1)).thenReturn("STAFF"); assertThrows(AccessDeniedException.class,()->service.getFinancial(20,1)); verifyNoInteractions(payments,refunds,bookings); }
    @Test void revokedActorIsRejectedAfterBookingLock() { when(access.currentRole(1)).thenReturn("ADMIN","STAFF"); assertThrows(AccessDeniedException.class,()->service.createRefund(create(),1,"管理員")); verify(refunds,never()).insert(any()); }
    @Test void identicalCreateRetryReturnsOriginalWithoutAnotherReservation() { when(refunds.findByKey(key)).thenReturn(refund); RefundCreateRequest request = create(); request.setExpectedVersion(1L); assertSame(refund,service.createRefund(request,1,"管理員")); verify(refunds,never()).insert(any()); verify(payments,never()).rotateFinancialVersion(any()); }
    @Test void reusedKeyWithChangedAmountFails() { when(refunds.findByKey(key)).thenReturn(refund); RefundCreateRequest request = create(); request.setAmount(BigDecimal.ONE); assertThrows(BookingConflictException.class,()->service.createRefund(request,1,"管理員")); }
    @Test void pendingRequestsReserveRefundCapacity() { when(refunds.sumCommitted(20)).thenReturn(new BigDecimal("900")); assertThrows(BookingConflictException.class,()->service.createRefund(create(),1,"管理員")); }
    @Test void activeBookingCannotRequestOrdinaryRefund() { booking.setStatus("confirmed"); assertThrows(BookingConflictException.class,()->service.createRefund(create(),1,"管理員")); }
    @Test void newRequestWritesActorHistoryAndVersion() { service.createRefund(create(),1,"管理員"); verify(refunds).insert(argThat(record -> record.getRequestedBy()==1 && record.getRequestedAt().equals(OffsetDateTime.now(clock)))); verify(bookings).insertBookingTimeline(argThat(t -> t.getActorUserId()==1 && t.getActionType().equals("refund_requested"))); verify(payments).rotateFinancialVersion(20); }
    @Test void cannotCompleteBeforeApproval() { assertThrows(BookingConflictException.class,()->service.updateRefund(30,action("complete_manual"),1,"管理員")); verify(refunds,never()).update(any()); }
    @Test void simulatedRefundNeverDeductsActualMoney() { refund.setStatus("approved"); service.updateRefund(30,action("simulate"),1,"管理員"); assertEquals("simulated",refund.getStatus()); verify(bookings,never()).updateBookingPaidAmount(any(),any()); }
    @Test void manualCompletionRequiresProof() { refund.setStatus("approved"); RefundActionRequest request=action("complete_manual"); request.setExternalReference(" "); assertThrows(IllegalArgumentException.class,()->service.updateRefund(30,request,1,"管理員")); }
    @Test void manualCompletionRefusesLegacyLedgerMismatch() { refund.setStatus("approved"); booking.setPaidAmount(BigDecimal.ONE); assertThrows(BookingConflictException.class,()->service.updateRefund(30,action("complete_manual"),1,"管理員")); verify(refunds,never()).update(any()); }
    @Test void completedRefundIsTerminal() { refund.setStatus("completed"); assertThrows(BookingConflictException.class,()->service.updateRefund(30,action("simulate"),1,"管理員")); }
    @Test void approvalStoresActorAndDecision() { service.updateRefund(30,action("approve"),1,"管理員"); assertEquals("approved",refund.getStatus()); assertEquals(1,refund.getDecidedBy()); assertEquals(OffsetDateTime.now(clock),refund.getDecidedAt()); }
    @Test void staleRefundVersionNeverOverwritesApproval() { RefundActionRequest request=action("approve"); request.setExpectedVersion(1L); assertThrows(BookingConflictException.class,()->service.updateRefund(30,request,1,"管理員")); }
    @Test void latePaymentCanBeAssignedToRefundWithoutBookingCredit() { payment.setStatus("paid_review"); service.reconcile(20,reconcile("refund_required"),1,"管理員"); assertEquals("refund_required",payment.getReconciliationStatus()); verify(bookings,never()).updateBookingPaidAmount(any(),any()); }
    @Test void staleBookingVersionCannotReconcileMoney() { payment.setStatus("paid_review"); PaymentReconciliationRequest request=reconcile("accept"); request.setExpectedBookingVersion(7L); assertThrows(BookingConflictException.class,()->service.reconcile(20,request,1,"管理員")); }
    @Test void acceptanceDoesNotReopenCancelledBookingOrTouchInventory() { payment.setStatus("paid_review"); service.reconcile(20,reconcile("accept"),1,"管理員"); assertEquals("paid",payment.getStatus()); verify(bookings,never()).updateBookingStatus(any(),any()); }
    @Test void sameProofCannotCompleteAnotherRefund() { refund.setStatus("approved"); when(refunds.countReference(20,"CLOUD-PROOF")).thenReturn(1); assertThrows(BookingConflictException.class,()->service.updateRefund(30,action("complete_manual"),1,"管理員")); }
    @Test void ledgerSynchronizationRequiresPaymentAndBookingVersions() { booking.setPaidAmount(BigDecimal.ONE); service.reconcile(20,reconcile("sync_ledger"),1,"管理員"); verify(bookings).updateBookingPaidAmount(10,new BigDecimal("1000")); assertEquals("ledger_checked",payment.getReconciliationStatus()); }
}
