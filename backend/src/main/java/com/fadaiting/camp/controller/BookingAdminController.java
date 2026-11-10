package com.fadaiting.camp.controller;

import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.dto.BookingNoteUpdateRequest;
import com.fadaiting.camp.dto.BookingStayUpdateRequest;
import com.fadaiting.camp.service.BookingStayService;
import com.fadaiting.camp.pojo.Booking;

import com.fadaiting.camp.dto.BookingAdminQueryDto;
import com.fadaiting.camp.dto.BookingAdminRowDto;
import com.fadaiting.camp.dto.BookingAdminUpdateRequest;
import com.fadaiting.camp.dto.BookingDetailDto;
import com.fadaiting.camp.dto.BookingStatusUpdateRequest;
import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/booking")
public class BookingAdminController {
    @Autowired
    private BookingService bookingService;
    @Autowired
    private CampAccessService campAccessService;

    @Autowired
    private BookingStayService bookingStayService;

    @Autowired private com.fadaiting.camp.service.BookingHoldService bookingHoldService;

    @PostMapping("/expire-unpaid")
    public Result<com.fadaiting.camp.dto.BookingExpirationResponse> expireUnpaid(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (!"ADMIN".equals(campAccessService.currentRole(currentUser.getId()))) throw new org.springframework.security.access.AccessDeniedException("僅管理員可執行逾時批次");
        return Result.success(bookingHoldService.expireBatch());
    }

    @PatchMapping("/stay")
    public Result<Booking> updateStay(@RequestBody @Valid BookingStayUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return Result.success(bookingStayService.updateStay(request, currentUser.getId(), currentUser.getUsername()));
    }

    @PostMapping("/list")
    public Result<PageBean<BookingAdminRowDto>> list(@RequestBody BookingAdminQueryDto queryDto) {
        return Result.success(bookingService.getAdminBookings(queryDto));
    }

    @GetMapping("/detail/{bookingId}")
    public Result<BookingDetailDto> detail(@PathVariable Integer bookingId,
                                         @AuthenticationPrincipal CustomUserDetails currentUser) {
        BookingDetailDto detail = bookingService.getAdminBookingDetail(bookingId);
        if (!"ADMIN".equals(campAccessService.currentRole(currentUser.getId()))) {
            detail.setTimeline(detail.getTimeline().stream()
                    .filter(entry -> !entry.getActionType().startsWith("refund_") && !"payment_reconciled".equals(entry.getActionType()))
                    .toList());
            detail.getPayments().forEach(payment -> {
                payment.setCallbackPayload(null);
                payment.setPaymentUrl(null);
                payment.setOperationVersion(null);
                payment.setReconciliationNote(null);
                payment.setReconciliationReference(null);
                payment.setReconciledBy(null);
                payment.setReconciledAt(null);
            });
        }
        return Result.success(detail);
    }

    @PatchMapping("/status")
    public Result<Void> updateStatus(
            @RequestBody @Valid BookingStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        bookingService.updateBookingStatus(request, currentUser.getId(), currentUser.getUsername());
        return Result.success();
    }

    @PatchMapping("/detail")
    public Result<Void> updateDetail(
            @RequestBody @Valid BookingAdminUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        bookingService.updateAdminBooking(request, currentUser.getId(), currentUser.getUsername());
        return Result.success();
    }

    @PatchMapping("/note")
    public Result<Void> updateNote(@RequestBody @Valid BookingNoteUpdateRequest request,
                                   @AuthenticationPrincipal CustomUserDetails currentUser) {
        bookingService.updateBookingNote(request, currentUser.getId(), currentUser.getUsername());
        return Result.success();
    }
}
