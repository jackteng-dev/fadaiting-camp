package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.dto.*;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.mapper.*;
import com.fadaiting.camp.pojo.*;
import com.fadaiting.camp.service.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.Set;

@Service
public class PaymentFinancialServiceImpl implements PaymentFinancialService {
    private final BookingMapper bookings;
    private final PaymentMapper payments;
    private final RefundMapper refunds;
    private final CampAccessService access;
    private final Clock clock;
    public PaymentFinancialServiceImpl(BookingMapper bookings, PaymentMapper payments, RefundMapper refunds, CampAccessService access, Clock clock) {
        this.bookings = bookings; this.payments = payments; this.refunds = refunds; this.access = access; this.clock = clock;
    }
    private void requireAdmin(Integer actorId) {
        if (!"ADMIN".equals(access.currentRole(actorId))) throw new AccessDeniedException("僅管理員可處理退款與對帳");
    }
    private Booking lockBooking(Integer paymentId, Integer actorId) {
        requireAdmin(actorId);
        PaymentRecord payment = payments.findById(paymentId);
        if (payment == null) throw new IllegalArgumentException("找不到付款紀錄");
        Booking booking = bookings.findBookingForUpdate(payment.getBookingId());
        requireAdmin(actorId);
        if (booking == null) throw new IllegalArgumentException("找不到訂單");
        return booking;
    }
    @Override @Transactional public PaymentFinancialResponse getFinancial(Integer paymentId, Integer actorId) {
        Booking booking = lockBooking(paymentId, actorId);
        return response(payments.findById(paymentId), booking);
    }
    private boolean eligible(PaymentRecord payment, Booking booking) {
        return ("paid".equals(payment.getStatus()) && Set.of("cancelled","completed").contains(booking.getStatus()))
                || ("paid_review".equals(payment.getStatus()) && "refund_required".equals(payment.getReconciliationStatus()));
    }
    private PaymentFinancialResponse response(PaymentRecord payment, Booking booking) {
        BigDecimal completed = refunds.sumCompleted(payment.getId()), committed = refunds.sumCommitted(payment.getId());
        PaymentFinancialResponse response = new PaymentFinancialResponse();
        response.setPaymentRecordId(payment.getId()); response.setBookingStatus(booking.getStatus()); response.setPaymentStatus(payment.getStatus());
        response.setCanRequestRefund(eligible(payment, booking)); response.setOperationVersion(payment.getOperationVersion());
        response.setBookingOperationVersion(booking.getOperationVersion());
        response.setReconciliationStatus(payment.getReconciliationStatus()); response.setReconciliationNote(payment.getReconciliationNote());
        response.setReconciliationReference(payment.getReconciliationReference()); response.setReconciledBy(payment.getReconciledBy()); response.setReconciledAt(payment.getReconciledAt());
        response.setAmount(payment.getAmount()); response.setCompletedRefundAmount(completed); response.setReservedRefundAmount(committed.subtract(completed));
        response.setAvailableRefundAmount(payment.getAmount().subtract(committed).max(BigDecimal.ZERO));
        response.setLedgerPaidAmount(payments.sumPaidAmountByBookingId(booking.getId())); response.setBookingPaidAmount(booking.getPaidAmount());
        response.setRefunds(refunds.findByPayment(payment.getId())); return response;
    }
    private void requireVersion(Long current, Long expected) {
        if (expected == null || !expected.equals(current)) throw conflict("付款或退款已更新，請重新載入後再操作");
    }
    private BookingConflictException conflict(String message) { return new BookingConflictException(message); }
    private String text(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("請填寫" + label);
        return value.trim();
    }
    @Override @Transactional public RefundRecord createRefund(RefundCreateRequest request, Integer actorId, String actorName) {
        Booking booking = lockBooking(request.getPaymentRecordId(), actorId);
        PaymentRecord payment = payments.findById(request.getPaymentRecordId());
        String reason = text(request.getReason(), "退款原因");
        if (request.getAmount() == null || request.getAmount().signum() <= 0 || request.getAmount().scale() > 2) throw new IllegalArgumentException("請填寫有效退款金額，最多兩位小數");
        if (request.getIdempotencyKey() == null) throw new IllegalArgumentException("缺少退款請求識別碼");
        RefundRecord previous = refunds.findByKey(request.getIdempotencyKey());
        if (previous != null) {
            if (!previous.getPaymentRecordId().equals(payment.getId()) || previous.getAmount().compareTo(request.getAmount()) != 0 || !previous.getReason().equals(reason))
                throw conflict("相同請求識別碼已用於不同退款內容");
            return previous;
        }
        requireVersion(payment.getOperationVersion(), request.getExpectedVersion());
        if (!eligible(payment, booking)) throw conflict("一般收款需先取消或完成預約；待核對收款須先決定安排退款");
        if (!"TWD".equals(payment.getCurrency())) throw conflict("目前僅支援新台幣退款");
        BigDecimal available = payment.getAmount().subtract(refunds.sumCommitted(payment.getId()));
        if (request.getAmount().compareTo(available) > 0) throw conflict("退款金額超過剩餘可退金額，待審核申請也會保留額度");
        RefundRecord refund = new RefundRecord(); refund.setPaymentRecordId(payment.getId()); refund.setAmount(request.getAmount());
        refund.setReason(reason); refund.setIdempotencyKey(request.getIdempotencyKey()); refund.setRequestedBy(actorId); refund.setRequestedAt(OffsetDateTime.now(clock));
        try { refunds.insert(refund); } catch (DuplicateKeyException error) { throw conflict("退款請求已建立，請重新載入核對"); }
        payments.rotateFinancialVersion(payment.getId());
        timeline(booking.getId(), "refund_requested", "申請退款", actorId, actorName, "付款紀錄 #"+payment.getId()+"，申請退款 "+refund.getAmount()+"；原因："+reason);
        return refunds.findById(refund.getId());
    }
    @Override @Transactional public RefundRecord updateRefund(Integer refundId, RefundActionRequest request, Integer actorId, String actorName) {
        requireAdmin(actorId);
        RefundRecord candidate = refunds.findById(refundId);
        if (candidate == null) throw new IllegalArgumentException("找不到退款申請");
        Booking booking = lockBooking(candidate.getPaymentRecordId(), actorId);
        RefundRecord refund = refunds.findById(refundId); PaymentRecord payment = payments.findById(refund.getPaymentRecordId());
        requireVersion(refund.getOperationVersion(), request.getExpectedVersion());
        String note = text(request.getNote(), "處理說明"); String action = request.getAction(); OffsetDateTime now = OffsetDateTime.now(clock);
        if (Set.of("approve","reject").contains(action)) {
            if (!"requested".equals(refund.getStatus())) throw conflict("只有待審核退款可核准或駁回");
            refund.setStatus("approve".equals(action) ? "approved" : "rejected"); refund.setDecidedBy(actorId); refund.setDecidedAt(now);
        } else if (Set.of("complete_manual","simulate").contains(action)) {
            if (!"approved".equals(refund.getStatus())) throw conflict("退款需先通過審核，且完成後不得再次處理");
            if ("complete_manual".equals(action)) {
                String reference = text(request.getExternalReference(), "實際退款憑證編號").toUpperCase(java.util.Locale.ROOT);
                if (refunds.countReference(payment.getId(), reference) != 0) throw conflict("此付款的退款憑證編號已使用");
                if ("paid".equals(payment.getStatus())) requireLedgerMatch(booking);
                refund.setStatus("completed"); refund.setExternalReference(reference);
            } else refund.setStatus("simulated");
            refund.setCompletedBy(actorId); refund.setCompletedAt(now);
        } else throw new IllegalArgumentException("不支援的退款操作");
        refund.setDecisionNote(note); refunds.update(refund); payments.rotateFinancialVersion(payment.getId());
        if ("completed".equals(refund.getStatus()) && "paid".equals(payment.getStatus()))
            bookings.updateBookingPaidAmount(booking.getId(), payments.sumPaidAmountByBookingId(booking.getId()));
        String label = switch (action) { case "approve" -> "核准退款"; case "reject" -> "駁回退款"; case "simulate" -> "模擬退款完成（未退เงินจริง）"; default -> "登記人工退款完成"; };
        timeline(booking.getId(), "refund_"+refund.getStatus(), label, actorId, actorName, "退款 #"+refund.getId()+"，金額 "+refund.getAmount()+"；"+note);
        return refunds.findById(refundId);
    }
    private void requireLedgerMatch(Booking booking) {
        BigDecimal ledger = payments.sumPaidAmountByBookingId(booking.getId());
        if (ledger.compareTo(booking.getPaidAmount()) != 0) throw conflict("訂單已收金額與收退款紀錄不一致，請先人工核對，不自動覆蓋舊帳");
    }
    @Override @Transactional public PaymentFinancialResponse reconcile(Integer paymentId, PaymentReconciliationRequest request, Integer actorId, String actorName) {
        Booking booking = lockBooking(paymentId, actorId); PaymentRecord payment = payments.findById(paymentId);
        requireVersion(payment.getOperationVersion(), request.getExpectedVersion());
        requireVersion(booking.getOperationVersion(), request.getExpectedBookingVersion());
        boolean synchronize = "sync_ledger".equals(request.getResolution());
        if (synchronize ? !"paid".equals(payment.getStatus()) : (!"paid_review".equals(payment.getStatus()) || !"unreviewed".equals(payment.getReconciliationStatus()))) throw conflict("此收款已核對或不適用此核對操作");
        if (!"TWD".equals(payment.getCurrency())) throw conflict("目前僅支援新台幣核對");
        String note = text(request.getNote(), "核對說明"), reference = text(request.getExternalReference(), "查單憑證編號").toUpperCase(java.util.Locale.ROOT);
        if (synchronize) {
            payment.setReconciliationStatus("ledger_checked");
        } else if ("accept".equals(request.getResolution())) {
            requireLedgerMatch(booking); payment.setStatus("paid"); payment.setReconciliationStatus("accepted");
        } else if ("refund_required".equals(request.getResolution())) payment.setReconciliationStatus("refund_required");
        else throw new IllegalArgumentException("不支援的核對決定");
        payment.setReconciliationNote(note); payment.setReconciliationReference(reference); payment.setReconciledBy(actorId); payment.setReconciledAt(OffsetDateTime.now(clock));
        payments.updateReconciliation(payment);
        if (synchronize || "accepted".equals(payment.getReconciliationStatus())) {
            BigDecimal net = payments.sumPaidAmountByBookingId(booking.getId()); bookings.updateBookingPaidAmount(booking.getId(), net); booking.setPaidAmount(net);
        }
        timeline(booking.getId(), "payment_reconciled", "人工核對收款", actorId, actorName,
                "付款 #"+paymentId+"，"+(synchronize ? "核對並同步付款淨額" : "accepted".equals(payment.getReconciliationStatus()) ? "確認歸帳" : "安排退款")+"；憑證："+reference+"；"+note);
        return response(payments.findById(paymentId), bookings.findBookingForUpdate(booking.getId()));
    }
    private void timeline(Integer bookingId, String action, String label, Integer actorId, String actorName, String detail) {
        BookingTimeline entry = new BookingTimeline(); entry.setBookingId(bookingId); entry.setActionType(action); entry.setActionLabel(label);
        entry.setActorUserId(actorId); entry.setActorName(actorName); entry.setDetail(detail);
        entry.setCreatedTime(LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault())); bookings.insertBookingTimeline(entry);
    }
}
