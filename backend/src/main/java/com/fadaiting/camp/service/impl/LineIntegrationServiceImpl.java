package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.dto.LineBindConfirmRequest;
import com.fadaiting.camp.dto.LineBindConfirmResponse;
import com.fadaiting.camp.dto.LineBindRequestRequest;
import com.fadaiting.camp.dto.LineBindRequestResponse;
import com.fadaiting.camp.dto.LineBookingDetailRequest;
import com.fadaiting.camp.dto.LineBookingListRequest;
import com.fadaiting.camp.dto.LineBookingListResponse;
import com.fadaiting.camp.dto.PublicBookingLookupResponse;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.mapper.LineUserBindingMapper;
import com.fadaiting.camp.mapper.PaymentMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingGuest;
import com.fadaiting.camp.pojo.LineUserBinding;
import com.fadaiting.camp.service.LineIntegrationService;
import com.fadaiting.camp.service.LineIdentityVerifier;
import com.fadaiting.camp.expection.BookingConflictException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import com.fadaiting.camp.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LineIntegrationServiceImpl implements LineIntegrationService {
    private static final Set<String> PUBLIC_TIMELINE_ACTIONS = Set.of(
            "submitted",
            "booking_expired",
            "status_updated",
            "payment_confirmed",
            "payment_cancelled",
            "payment_failed",
            "payment_expired"
    );

    @Autowired
    private BookingMapper bookingMapper;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private LineUserBindingMapper lineUserBindingMapper;
    @Autowired
    private PaymentService paymentService;
    @Autowired private com.fadaiting.camp.service.BookingHoldService bookingHoldService;

    @Autowired private LineIdentityVerifier identityVerifier;
    @Autowired private Clock clock;

    @Override @Transactional
    public LineBindRequestResponse createBindRequest(LineBindRequestRequest request) {
        LineIdentityVerifier.Identity identity = identityVerifier.verify(request.getIdToken());
        Booking booking = requireBookingByNo(request.getBookingNo());
        booking = bookingMapper.findBookingForUpdate(booking.getId());
        BookingGuest guest = requireGuestByBookingId(booking.getId());
        if (!normalizePhone(guest.getContactPhone()).equals(normalizePhone(request.getContactPhone()))) {
            throw new IllegalArgumentException("查無符合資料，請確認訂單編號與聯絡電話");
        }

        if (lineUserBindingMapper.findVerifiedByBooking(booking.getId()) != null) throw new BookingConflictException("此預約已完成 LINE 綁定，如需變更請聯絡營區");
        LocalDateTime now = LocalDateTime.now(clock);
        LineUserBinding binding = new LineUserBinding();
        binding.setLineUserId(identity.userId());
        binding.setBookingId(booking.getId());
        binding.setBookingNo(booking.getBookingNo());
        binding.setContactName(guest.getContactName());
        binding.setContactPhone(guest.getContactPhone());
        binding.setContactEmail(guest.getContactEmail());
        binding.setStatus("pending");
        binding.setBindToken(UUID.randomUUID().toString());
        binding.setBindTokenExpireTime(now.plusMinutes(10));
        binding.setCreatedTime(now);
        binding.setModifiedTime(now);
        lineUserBindingMapper.insert(binding);

        LineBindRequestResponse response = new LineBindRequestResponse();
        response.setBindingId(binding.getId());
        response.setBindToken(binding.getBindToken());
        response.setExpiresAt(binding.getBindTokenExpireTime());
        response.setBookingId(booking.getId());
        response.setBookingNo(booking.getBookingNo());
        response.setContactName(guest.getContactName());
        response.setMaskedContactPhone(maskPhone(guest.getContactPhone()));
        return response;
    }

    @Override @Transactional
    public LineBindConfirmResponse confirmBind(LineBindConfirmRequest request) {
        LineIdentityVerifier.Identity identity = identityVerifier.verify(request.getIdToken());
        LineUserBinding candidate = lineUserBindingMapper.findByBindToken(normalize(request.getBindToken()));
        if (candidate == null || candidate.getBookingId() == null) throw new BookingConflictException("綁定請求不存在或已失效");
        if (bookingMapper.findBookingForUpdate(candidate.getBookingId()) == null) throw new BookingConflictException("找不到預約");
        LineUserBinding binding = lineUserBindingMapper.findForUpdate(candidate.getId());
        if (binding == null || !"pending".equals(binding.getStatus()) || !normalize(request.getBindToken()).equals(binding.getBindToken()))
            throw new BookingConflictException("綁定請求不存在或已失效");
        if (!identity.userId().equals(binding.getLineUserId())) throw new AccessDeniedException("綁定請求與 LINE 帳號不符，請使用原帳號重新操作");
        if (binding.getBindTokenExpireTime() == null || !binding.getBindTokenExpireTime().isAfter(LocalDateTime.now(clock)))
            throw new BookingConflictException("綁定請求已逾時，請重新操作");
        if (lineUserBindingMapper.findVerifiedByBooking(binding.getBookingId()) != null)
            throw new BookingConflictException("此預約已完成 LINE 綁定，如需變更請聯絡營區");
        lineUserBindingMapper.deactivateUnverifiedForBooking(binding.getBookingId());
        LocalDateTime now = LocalDateTime.now(clock);
        binding.setDisplayName(identity.displayName());
        binding.setPictureUrl(identity.pictureUrl());
        binding.setStatus("active");
        binding.setBindToken("");
        binding.setBindTokenExpireTime(null);
        binding.setIdentityVerifiedAt(OffsetDateTime.now(clock));
        binding.setBoundAt(now);
        binding.setLastInteractionTime(now);
        binding.setModifiedTime(now);
        lineUserBindingMapper.updateBinding(binding);

        LineBindConfirmResponse response = new LineBindConfirmResponse();
        response.setBindingId(binding.getId());
        response.setLineUserId(binding.getLineUserId());
        response.setDisplayName(binding.getDisplayName());
        response.setContactName(binding.getContactName());
        response.setMaskedContactPhone(maskPhone(binding.getContactPhone()));
        response.setStatus(binding.getStatus());
        response.setBookingId(binding.getBookingId());
        response.setBookingNo(binding.getBookingNo());
        response.setBoundAt(binding.getBoundAt());
        return response;
    }

    @Override
    public LineBookingListResponse getLineBookings(LineBookingListRequest request) {
        LineIdentityVerifier.Identity identity = identityVerifier.verify(request.getIdToken());
        LineUserBinding binding = requireActiveBinding(identity.userId());
        List<Booking> bookings = bookingMapper.findBookingsForVerifiedLineUser(identity.userId());

        LineBookingListResponse response = new LineBookingListResponse();
        response.setLineUserId(binding.getLineUserId());
        response.setDisplayName(binding.getDisplayName());
        response.setContactName(binding.getContactName());
        response.setMaskedContactPhone(maskPhone(binding.getContactPhone()));
        response.setBookings(bookings.stream().map(booking -> {
            LineBookingListResponse.Item item = new LineBookingListResponse.Item();
            item.setBookingId(booking.getId());
            item.setBookingNo(booking.getBookingNo());
            item.setStatus(booking.getStatus());
            item.setCheckInDate(booking.getCheckInDate());
            item.setCheckOutDate(booking.getCheckOutDate());
            item.setNights(booking.getNights());
            item.setTotalAmount(booking.getTotalAmount());
            item.setPaidAmount(booking.getPaidAmount());
            item.setCreatedTime(booking.getCreatedTime());
            return item;
        }).toList());
        return response;
    }

    @Override
    public PublicBookingLookupResponse getLineBookingDetail(LineBookingDetailRequest request) {
        LineIdentityVerifier.Identity identity = identityVerifier.verify(request.getIdToken());
        LineUserBinding binding = lineUserBindingMapper.findVerifiedForUserBooking(identity.userId(),request.getBookingId());
        if (binding == null) throw new AccessDeniedException("此預約尚未綁定至目前 LINE 帳號");
        Booking booking = bookingMapper.findBookingById(request.getBookingId());
        if (booking == null) {
            throw new IllegalArgumentException("查無符合資料");
        }
        BookingGuest guest = requireGuestByBookingId(booking.getId());
        if (!safe(guest.getContactPhone()).equals(safe(binding.getContactPhone()))) {
            throw new IllegalArgumentException("查無符合資料");
        }
        return buildPublicBookingResponse(booking, guest);
    }

    private PublicBookingLookupResponse buildPublicBookingResponse(Booking booking, BookingGuest guest) {
        bookingHoldService.expireBooking(booking.getId());
        booking = bookingMapper.findBookingById(booking.getId());
        paymentService.expireStalePendingPayments(booking.getId());
        PublicBookingLookupResponse response = new PublicBookingLookupResponse();
        response.setBookingId(booking.getId());
        response.setBookingNo(booking.getBookingNo());
        response.setStatus(booking.getStatus());
        response.setPaymentDueAt(booking.getPaymentDueAt());
        response.setExpiredAt(booking.getExpiredAt());
        response.setStayStatus(booking.getStayStatus());
        response.setCheckedInAt(booking.getCheckedInAt());
        response.setCheckedOutAt(booking.getCheckedOutAt());
        response.setCheckInDate(booking.getCheckInDate());
        response.setCheckOutDate(booking.getCheckOutDate());
        response.setNights(booking.getNights());
        response.setGuestCount(booking.getGuestCount());
        response.setVehicleCount(booking.getVehicleCount());
        response.setSubtotalAmount(booking.getSubtotalAmount());
        response.setAdjustmentAmount(booking.getAdjustmentAmount());
        response.setTotalAmount(booking.getTotalAmount());
        response.setPaidAmount(booking.getPaidAmount());
        response.setContactName(guest == null ? "" : guest.getContactName());
        response.setMaskedContactPhone(guest == null ? "" : maskPhone(guest.getContactPhone()));
        response.setCreatedTime(booking.getCreatedTime());
        response.setItems(bookingMapper.findBookingItemsByBookingId(booking.getId()).stream().map(item -> {
            PublicBookingLookupResponse.Item publicItem = new PublicBookingLookupResponse.Item();
            publicItem.setItemName(item.getItemName());
            publicItem.setQuantity(item.getQuantity());
            publicItem.setNights(item.getNights());
            publicItem.setLineTotal(item.getLineTotal());
            return publicItem;
        }).toList());
        response.setPayments(paymentMapper.findByBookingId(booking.getId()).stream().map(payment -> {
            PublicBookingLookupResponse.Payment publicPayment = new PublicBookingLookupResponse.Payment();
            publicPayment.setProvider(payment.getProvider());
            publicPayment.setPaymentMethod(payment.getPaymentMethod());
            publicPayment.setStatus(payment.getStatus());
            publicPayment.setAmount(payment.getAmount());
            publicPayment.setRefundedAmount(payment.getRefundedAmount());
            publicPayment.setCurrency(payment.getCurrency());
            publicPayment.setPaidTime(payment.getPaidTime());
            publicPayment.setCreatedTime(payment.getCreatedTime());
            return publicPayment;
        }).toList());
        response.setTimeline(bookingMapper.findBookingTimelineByBookingId(booking.getId()).stream()
                .filter(entry -> PUBLIC_TIMELINE_ACTIONS.contains(entry.getActionType()))
                .map(entry -> {
                    PublicBookingLookupResponse.Timeline publicTimeline = new PublicBookingLookupResponse.Timeline();
                    publicTimeline.setActionType(entry.getActionType());
                    publicTimeline.setActionLabel(entry.getActionLabel());
                    publicTimeline.setDetail(entry.getDetail());
                    publicTimeline.setCreatedTime(entry.getCreatedTime());
                    return publicTimeline;
                })
                .toList());
        response.setRefundedAmount(response.getPayments().stream().map(PublicBookingLookupResponse.Payment::getRefundedAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        return response;
    }

    private LineUserBinding requireActiveBinding(String lineUserId) {
        LineUserBinding binding = lineUserBindingMapper.findActiveByLineUserId(normalize(lineUserId));
        if (binding == null) {
            throw new IllegalArgumentException("LINE 帳號尚未完成綁定");
        }
        return binding;
    }

    private Booking requireBookingByNo(String bookingNo) {
        Booking booking = bookingMapper.findBookingByBookingNo(normalize(bookingNo));
        if (booking == null) {
            throw new IllegalArgumentException("查無符合資料，請確認訂單編號與聯絡電話");
        }
        return booking;
    }

    private BookingGuest requireGuestByBookingId(Integer bookingId) {
        BookingGuest guest = bookingMapper.findBookingGuestByBookingId(bookingId);
        if (guest == null) {
            throw new IllegalArgumentException("找不到旅客資料");
        }
        return guest;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String maskPhone(String phone) {
        String normalized = safe(phone);
        if (normalized.length() <= 4) {
            return normalized;
        }
        return normalized.substring(0, Math.min(4, normalized.length()))
                + "*".repeat(Math.max(0, normalized.length() - 6))
                + normalized.substring(Math.max(0, normalized.length() - 2));
    }
}
