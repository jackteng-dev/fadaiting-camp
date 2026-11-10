package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.service.CampAccessService;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.utils.BookingStatePolicy;
import com.fadaiting.camp.dto.BookingNoteUpdateRequest;
import org.springframework.security.access.AccessDeniedException;

import com.fadaiting.camp.dto.BookingCreateRequest;
import com.fadaiting.camp.dto.BookingCreateResponse;
import com.fadaiting.camp.dto.BookingAdminQueryDto;
import com.fadaiting.camp.dto.BookingAdminRowDto;
import com.fadaiting.camp.dto.BookingAdminUpdateRequest;
import com.fadaiting.camp.dto.BookingDetailDto;
import com.fadaiting.camp.dto.BookingQuoteNightDto;
import com.fadaiting.camp.dto.BookingQuoteRentalItemDto;
import com.fadaiting.camp.dto.BookingQuoteRequest;
import com.fadaiting.camp.dto.BookingQuoteResponse;
import com.fadaiting.camp.dto.BookingRentalItemRequest;
import com.fadaiting.camp.dto.BookingStatusUpdateRequest;
import com.fadaiting.camp.dto.PublicBookingLookupRequest;
import com.fadaiting.camp.dto.PublicBookingLookupResponse;
import com.fadaiting.camp.dto.PublicBookingSummaryRequest;
import com.fadaiting.camp.mapper.AvailabilityMapper;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.mapper.PaymentMapper;
import com.fadaiting.camp.mapper.RentalItemMapper;
import com.fadaiting.camp.pojo.Availability;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingGuest;
import com.fadaiting.camp.pojo.BookingItem;
import com.fadaiting.camp.pojo.BookingTimeline;
import com.fadaiting.camp.pojo.CampUnit;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.RatePlan;
import com.fadaiting.camp.pojo.RentalItem;
import com.fadaiting.camp.service.BookingService;
import com.fadaiting.camp.service.NotificationService;
import com.fadaiting.camp.service.PaymentService;
import com.fadaiting.camp.utils.ListPageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {
    @Autowired
    private CampAccessService campAccessService;
    private static final Set<String> PUBLIC_TIMELINE_ACTIONS = Set.of(
            "submitted",
            "booking_expired",
            "checked_in",
            "checked_out",
            "status_updated",
            "payment_confirmed",
            "payment_cancelled",
            "payment_failed",
            "payment_expired"
    );

    @Autowired
    private BookingMapper bookingMapper;
    @Autowired private com.fadaiting.camp.service.BookingHoldService bookingHoldService;
    @Autowired private com.fadaiting.camp.service.BookingInventoryService bookingInventoryService;
    @Autowired private com.fadaiting.camp.mapper.RefundMapper refundMapper;
    @Autowired
    private AvailabilityMapper availabilityMapper;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private RentalItemMapper rentalItemMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private PaymentService paymentService;

    @Override
    public BookingQuoteResponse quotePublicBooking(BookingQuoteRequest request) {
        validateBookingRequest(request.getCheckInDate(), request.getCheckOutDate());

        CampUnit unit = requireBookableUnit(request.getCampUnitId());
        validateGuestAndVehicleCount(unit, request.getGuestCount(), request.getVehicleCount());

        return buildQuote(unit, request.getCheckInDate(), request.getCheckOutDate(), request.getGuestCount(), request.getVehicleCount(), request.getRentalItems());
    }

    @Override
    @Transactional
    public BookingCreateResponse createPublicBooking(BookingCreateRequest request) {
        validateBookingRequest(request.getCheckInDate(), request.getCheckOutDate());

        CampUnit unit = requireBookableUnit(request.getCampUnitId());
        validateGuestAndVehicleCount(unit, request.getGuestCount(), request.getVehicleCount());

        bookingInventoryService.lockRentals(request.getRentalItems() == null ? List.of() :
                request.getRentalItems().stream()
                        .filter(item -> item != null && item.getRentalItemId() != null && item.getQuantity() != null && item.getQuantity() > 0)
                        .map(BookingRentalItemRequest::getRentalItemId).toList());
        BookingQuoteResponse quote = buildQuote(
                unit,
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getGuestCount(),
                request.getVehicleCount(),
                request.getRentalItems()
        );
        if (!quote.isAvailable()) {
            throw new IllegalArgumentException(quote.getMessage());
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal subtotal = quote.getTotalAmount();

        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setBookingNo(generateBookingNo());
        booking.setCampSiteId(unit.getCampSiteId());
        booking.setStatus("pending");
        booking.setPaymentDueAt(bookingHoldService.newDeadline());
        booking.setSource("website");
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNights((int) nights);
        booking.setGuestCount(request.getGuestCount());
        booking.setVehicleCount(request.getVehicleCount());
        booking.setSubtotalAmount(subtotal);
        booking.setAdjustmentAmount(BigDecimal.ZERO);
        booking.setTotalAmount(subtotal);
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setNote(request.getNote());
        booking.setCreatedTime(now);
        booking.setModifiedTime(now);
        bookingMapper.insertBooking(booking);

        BookingGuest bookingGuest = new BookingGuest();
        bookingGuest.setBookingId(booking.getId());
        bookingGuest.setContactName(request.getContactName());
        bookingGuest.setContactPhone(request.getContactPhone());
        bookingGuest.setContactEmail(request.getContactEmail());
        bookingGuest.setArrivalNote(request.getNote());
        bookingGuest.setCreatedTime(now);
        bookingGuest.setModifiedTime(now);
        bookingMapper.insertBookingGuest(bookingGuest);

        RatePlan ratePlan = findPreferredRatePlan(unit.getId(), request.getCheckInDate());
        BookingItem bookingItem = new BookingItem();
        bookingItem.setBookingId(booking.getId());
        bookingItem.setCampUnitId(unit.getId());
        bookingItem.setRatePlanId(ratePlan != null ? ratePlan.getId() : null);
        bookingItem.setItemName(unit.getName());
        bookingItem.setQuantity(1);
        bookingItem.setUnitPrice(resolveBookingItemUnitPrice(quote));
        bookingItem.setNights((int) nights);
        bookingItem.setLineTotal(subtotal);
        bookingItem.setCreatedTime(now);
        bookingItem.setModifiedTime(now);
        bookingMapper.insertBookingItem(bookingItem);

        if (quote.getRentalItems() != null) {
            for (BookingQuoteRentalItemDto rentalItemDto : quote.getRentalItems()) {
                BookingItem rentalBookingItem = new BookingItem();
                rentalBookingItem.setBookingId(booking.getId());
                rentalBookingItem.setCampUnitId(unit.getId());
                rentalBookingItem.setRatePlanId(null);
                rentalBookingItem.setRentalItemId(rentalItemDto.getRentalItemId());
                rentalBookingItem.setItemName("租借：" + rentalItemDto.getName());
                rentalBookingItem.setQuantity(rentalItemDto.getQuantity());
                rentalBookingItem.setUnitPrice(rentalItemDto.getUnitPrice());
                rentalBookingItem.setNights(1);
                rentalBookingItem.setLineTotal(rentalItemDto.getLineTotal());
                rentalBookingItem.setCreatedTime(now);
                rentalBookingItem.setModifiedTime(now);
                bookingMapper.insertBookingItem(rentalBookingItem);
            }
        }

        bookingInventoryService.reserve(unit, request.getCheckInDate(), request.getCheckOutDate(), 1);
        appendTimeline(
                booking.getId(),
                "submitted",
                "前台送出預約",
                null,
                "網站訪客",
                buildSubmittedDetail(booking, bookingGuest)
        );
        notificationService.queueBookingSubmittedNotification(booking, bookingGuest);

        return new BookingCreateResponse(
                booking.getBookingNo(),
                booking.getId(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getPaymentDueAt()
        );
    }

    @Override
    public PageBean<BookingAdminRowDto> getAdminBookings(BookingAdminQueryDto queryDto) {
        if (queryDto.getPageNum() == null || queryDto.getPageNum() < 1 || queryDto.getPageSize() == null || queryDto.getPageSize() < 1 || queryDto.getPageSize() > 500) throw new IllegalArgumentException("請使用有效分頁，每頁最多 500 筆");
        if ((queryDto.getStartDate() == null) != (queryDto.getEndDate() == null)) throw new IllegalArgumentException("請同時提供查詢起訖日期");
        if (queryDto.getStartDate() != null && (queryDto.getEndDate().isBefore(queryDto.getStartDate()) || ChronoUnit.DAYS.between(queryDto.getStartDate(), queryDto.getEndDate()) > 93)) throw new IllegalArgumentException("查詢日期需為有效範圍，最多 94 天");
        String bookingNo = normalize(queryDto.getBookingNo());
        String status = normalize(queryDto.getStatus());
        String contactName = normalize(queryDto.getContactName());

        ListPageUtil<BookingAdminRowDto> pageUtil = new ListPageUtil<>(
                bookingMapper.findAdminBookings(bookingNo, status, contactName, queryDto.getStartDate(), queryDto.getEndDate()),
                queryDto.getPageNum(),
                queryDto.getPageSize()
        );
        return new PageBean<>((long) pageUtil.getTotalCount(), pageUtil.getPageList());
    }

    @Override
    public BookingDetailDto getAdminBookingDetail(Integer bookingId) {
        Booking booking = bookingMapper.findBookingById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("找不到訂單資料");
        }
        bookingHoldService.expireBooking(bookingId);
        booking = bookingMapper.findBookingById(bookingId);
        paymentService.expireStalePendingPayments(bookingId);

        BookingDetailDto detailDto = new BookingDetailDto();
        detailDto.setBooking(booking);
        detailDto.setGuest(bookingMapper.findBookingGuestByBookingId(bookingId));
        detailDto.setItems(bookingMapper.findBookingItemsByBookingId(bookingId));
        detailDto.setTimeline(bookingMapper.findBookingTimelineByBookingId(bookingId));
        detailDto.setPayments(paymentMapper.findByBookingId(bookingId));
        return detailDto;
    }

    @Override
    public PublicBookingLookupResponse lookupPublicBooking(PublicBookingLookupRequest request) {
        String bookingNo = normalize(request.getBookingNo());
        Booking booking = bookingMapper.findBookingByBookingNo(bookingNo);
        if (booking == null) {
            throw new IllegalArgumentException("查無符合資料，請確認訂單編號與聯絡電話");
        }

        BookingGuest guest = bookingMapper.findBookingGuestByBookingId(booking.getId());
        if (guest == null || !normalizePhone(guest.getContactPhone()).equals(normalizePhone(request.getContactPhone()))) {
            throw new IllegalArgumentException("查無符合資料，請確認訂單編號與聯絡電話");
        }

        return buildPublicBookingResponse(booking, guest);
    }

    @Override
    public PublicBookingLookupResponse getPublicBookingSummary(PublicBookingSummaryRequest request) {
        Booking booking = bookingMapper.findBookingById(request.getBookingId());
        if (booking == null || !booking.getBookingNo().equalsIgnoreCase(normalize(request.getBookingNo()))) {
            throw new IllegalArgumentException("查無符合資料，請確認訂單資訊");
        }

        BookingGuest guest = bookingMapper.findBookingGuestByBookingId(booking.getId());
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
        response.setItems(bookingMapper.findBookingItemsByBookingId(booking.getId()).stream()
                .map(item -> {
                    PublicBookingLookupResponse.Item publicItem = new PublicBookingLookupResponse.Item();
                    publicItem.setItemName(item.getItemName());
                    publicItem.setQuantity(item.getQuantity());
                    publicItem.setNights(item.getNights());
                    publicItem.setLineTotal(item.getLineTotal());
                    return publicItem;
                })
                .toList());
        response.setPayments(paymentMapper.findByBookingId(booking.getId()).stream()
                .map(payment -> {
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
                })
                .toList());
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

    @Override
    @Transactional
    public void updateBookingStatus(BookingStatusUpdateRequest request, Integer actorUserId, String actorName) {
        if (!"ADMIN".equals(campAccessService.currentRole(actorUserId))) throw new AccessDeniedException("僅管理員可確認、取消或重新開啟預約；入住及退房請使用登記操作");
        if (!isValidStatus(request.getStatus())) throw new IllegalArgumentException("不支援的訂單狀態");
        Booking booking = bookingMapper.findBookingForUpdate(request.getBookingId());
        if (!"ADMIN".equals(campAccessService.currentRole(actorUserId))) throw new AccessDeniedException("無權限修改訂單狀態");
        if (booking == null) throw new IllegalArgumentException("找不到訂單資料");
        if (!booking.getOperationVersion().equals(request.getExpectedVersion())) throw new BookingConflictException("訂單已更新，請重新載入後再操作");
        if (request.getStatus().equals(booking.getStatus())) return;
        if (!BookingStatePolicy.allows(booking.getStatus(), booking.getStayStatus(), request.getStatus())) throw new BookingConflictException("不允許此狀態轉移；已入住需登記退房，完成訂單不能重新開啟");
        List<com.fadaiting.camp.dto.BookingDetailItemDto> items = bookingMapper.findBookingItemsByBookingId(booking.getId());
        if ("cancelled".equals(request.getStatus())) {
            bookingInventoryService.release(booking, items);
            bookingHoldService.invalidatePendingPayments(booking.getId());
        } else if ("cancelled".equals(booking.getStatus())) {
            if (refundMapper.countBlockingForBooking(booking.getId()) > 0) throw new BookingConflictException("此訂單有待處理或已完成退款，請另建預約；未執行退款可先駁回申請");
            bookingInventoryService.reopen(booking, items);
            bookingMapper.resetBookingHold(booking.getId(), "pending".equals(request.getStatus()) ? bookingHoldService.newDeadline() : null);
        } else if (bookingHoldService.isDue(booking) && "confirmed".equals(request.getStatus())) {
            throw new BookingConflictException("付款期限已到，請先取消，再核對庫存重新開啟預約");
        }
        String previousStatus = booking.getStatus();
        bookingMapper.updateBookingStatus(request.getBookingId(), request.getStatus());
        if (!request.getStatus().equals(previousStatus)) {
            appendTimeline(
                    request.getBookingId(),
                    "status_updated",
                    "更新訂單狀態",
                    actorUserId,
                    actorName,
                    String.format("狀態由「%s」調整為「%s」", translateStatus(previousStatus), translateStatus(request.getStatus()))
            );
        }
    }

    @Override
    @Transactional
    public void updateAdminBooking(BookingAdminUpdateRequest request, Integer actorUserId, String actorName) {
        if (!"ADMIN".equals(campAccessService.currentRole(actorUserId))) {
            throw new AccessDeniedException("僅管理員可調整金額與確認資訊");
        }
        Booking booking = bookingMapper.findBookingForUpdate(request.getBookingId());
        if (booking == null) {
            throw new IllegalArgumentException("找不到訂單資料");
        }

        if (!"ADMIN".equals(campAccessService.currentRole(actorUserId))) throw new AccessDeniedException("無權限調整訂單");
        if (!booking.getOperationVersion().equals(request.getExpectedVersion())) throw new BookingConflictException("訂單已更新，請重新載入後再操作");
        if (bookingHoldService.isDue(booking) && compareMoney(booking.getPaidAmount(), request.getPaidAmount()) != 0)
            throw new BookingConflictException("付款期限已到，請先取消並核對庫存");
        if ("cancelled".equals(booking.getStatus()) && compareMoney(booking.getPaidAmount(), request.getPaidAmount()) != 0)
            throw new BookingConflictException("已取消訂單的款項請由退款／人工核對流程處理");
        BigDecimal subtotal = booking.getSubtotalAmount() == null ? BigDecimal.ZERO : booking.getSubtotalAmount();
        BigDecimal adjustmentAmount = request.getAdjustmentAmount() == null ? BigDecimal.ZERO : request.getAdjustmentAmount();
        BigDecimal nextTotalAmount = subtotal.add(adjustmentAmount);

        if (nextTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("調整後總額不可小於 0");
        }

        List<String> changes = new ArrayList<>();
        if (compareMoney(booking.getPaidAmount(), request.getPaidAmount()) != 0) {
            changes.add(String.format("已收金額：%s -> %s", booking.getPaidAmount(), request.getPaidAmount()));
        }
        if (compareMoney(booking.getAdjustmentAmount(), adjustmentAmount) != 0) {
            changes.add(String.format("調整金額：%s -> %s", booking.getAdjustmentAmount(), adjustmentAmount));
        }
        if (!sameText(booking.getAdminNote(), request.getAdminNote())) {
            changes.add("更新內部備註");
        }
        if (!sameText(booking.getConfirmNote(), request.getConfirmNote())) {
            changes.add("更新人工確認備註");
        }
        if (!sameText(booking.getCancelReason(), request.getCancelReason())) {
            changes.add("更新取消原因");
        }

        bookingMapper.updateBookingAdminFields(
                request.getBookingId(),
                request.getPaidAmount(),
                adjustmentAmount,
                normalizeNote(request.getAdminNote()),
                normalizeNote(request.getConfirmNote()),
                normalizeNote(request.getCancelReason())
        );
        if (!changes.isEmpty()) {
            appendTimeline(
                    request.getBookingId(),
                    "booking_updated",
                    "更新訂單處理資訊",
                    actorUserId,
                    actorName,
                    String.join("；", changes)
            );
        }
    }

    private BookingQuoteResponse buildQuote(
            CampUnit unit,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer guestCount,
            Integer vehicleCount,
            List<BookingRentalItemRequest> rentalItemRequests
    ) {
        Map<LocalDate, Availability> availabilityMap = toAvailabilityMap(
                availabilityMapper.findByUnitAndDateRange(unit.getId(), checkInDate, checkOutDate)
        );

        int nights = (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        BigDecimal siteAmount = BigDecimal.ZERO;
        boolean available = true;
        String message = "目前日期可送出預約申請";
        List<BookingQuoteNightDto> nightsDetail = new java.util.ArrayList<>();

        for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
            Availability availability = availabilityMap.get(date);
            int effectiveTotal = availability != null ? availability.getTotalQuantity() : unit.getQuantity();
            int reserved = availability != null ? availability.getReservedQuantity() : 0;
            int blocked = availability != null ? availability.getBlockedQuantity() : 0;
            boolean closed = availability != null && Boolean.TRUE.equals(availability.getIsClosed());
            int remaining = Math.max(effectiveTotal - reserved - blocked, 0);
            BigDecimal nightlyPrice = resolveNightlyPrice(unit.getId(), date, availability);

            if (closed || remaining < 1) {
                available = false;
                message = String.format("%s 已無可售名額", date);
            }

            siteAmount = siteAmount.add(nightlyPrice);
            nightsDetail.add(new BookingQuoteNightDto(date, nightlyPrice, remaining, closed));
        }

        List<BookingQuoteRentalItemDto> rentalItems = buildRentalQuoteItems(
                unit.getCampSiteId(),
                checkInDate,
                checkOutDate,
                rentalItemRequests
        );
        for (BookingQuoteRentalItemDto rentalItem : rentalItems) {
            if (rentalItem.getAvailableQuantity() != null
                    && rentalItem.getQuantity() != null
                    && rentalItem.getQuantity() > rentalItem.getAvailableQuantity()) {
                available = false;
                message = String.format(
                        "%s 在這段日期最多可租 %d 件",
                        rentalItem.getName(),
                        rentalItem.getAvailableQuantity()
                );
                break;
            }
        }
        BigDecimal rentalAmount = rentalItems.stream()
                .map(BookingQuoteRentalItemDto::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BookingQuoteResponse.builder()
                .campUnitId(unit.getId())
                .campUnitName(unit.getName())
                .available(available)
                .nights(nights)
                .guestCount(guestCount)
                .vehicleCount(vehicleCount)
                .siteAmount(siteAmount)
                .rentalAmount(rentalAmount)
                .totalAmount(siteAmount.add(rentalAmount))
                .message(message)
                .nightsDetail(nightsDetail)
                .rentalItems(rentalItems)
                .build();
    }

    private List<BookingQuoteRentalItemDto> buildRentalQuoteItems(
            Integer campSiteId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            List<BookingRentalItemRequest> rentalItemRequests
    ) {
        if (rentalItemRequests == null || rentalItemRequests.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Integer> quantityById = new HashMap<>();
        for (BookingRentalItemRequest request : rentalItemRequests) {
            if (request.getRentalItemId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
                continue;
            }
            quantityById.merge(request.getRentalItemId(), request.getQuantity(), Integer::sum);
        }
        if (quantityById.isEmpty()) {
            return Collections.emptyList();
        }

        List<RentalItem> rentalItems = rentalItemMapper.findActiveRentalItemsByIds(
                campSiteId,
                new ArrayList<>(quantityById.keySet())
        );
        Map<Integer, RentalItem> rentalItemById = rentalItems.stream()
                .collect(Collectors.toMap(RentalItem::getId, Function.identity()));

        List<BookingQuoteRentalItemDto> quoteItems = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : quantityById.entrySet()) {
            RentalItem rentalItem = rentalItemById.get(entry.getKey());
            if (rentalItem == null) {
                throw new IllegalArgumentException("租借品不存在或未啟用");
            }
            Integer quantity = entry.getValue();
            int totalQuantity = rentalItem.getTotalQuantity() == null ? 0 : rentalItem.getTotalQuantity();
            Integer reservedQuantityValue = rentalItemMapper.findMaxReservedQuantityByRentalItemAndDateRange(
                    rentalItem.getId(),
                    checkInDate,
                    checkOutDate
            );
            int reservedQuantity = reservedQuantityValue == null ? 0 : reservedQuantityValue;
            int availableQuantity = Math.max(totalQuantity - reservedQuantity, 0);

            BookingQuoteRentalItemDto quoteItem = new BookingQuoteRentalItemDto();
            quoteItem.setRentalItemId(rentalItem.getId());
            quoteItem.setName(rentalItem.getName());
            quoteItem.setCategory(rentalItem.getCategory());
            quoteItem.setImageUrl(rentalItem.getImageUrl());
            quoteItem.setQuantity(quantity);
            quoteItem.setTotalQuantity(totalQuantity);
            quoteItem.setReservedQuantity(reservedQuantity);
            quoteItem.setAvailableQuantity(availableQuantity);
            quoteItem.setUnitPrice(rentalItem.getUnitPrice());
            quoteItem.setLineTotal(rentalItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            quoteItems.add(quoteItem);
        }
        return quoteItems;
    }

    private BigDecimal resolveNightlyPrice(Integer campUnitId, LocalDate checkInDate, Availability availability) {
        if (availability != null && availability.getPriceOverride() != null) {
            return availability.getPriceOverride();
        }
        RatePlan plan = findPreferredRatePlan(campUnitId, checkInDate);
        if (plan == null) {
            return BigDecimal.ZERO;
        }
        return plan.getBasePrice().add(plan.getCleaningFee() == null ? BigDecimal.ZERO : plan.getCleaningFee());
    }

    private RatePlan findPreferredRatePlan(Integer campUnitId, LocalDate date) {
        String dayType = isHoliday(date) ? "holiday" : "weekday";
        RatePlan exact = bookingMapper.findRatePlan(campUnitId, dayType);
        if (exact != null) {
            return exact;
        }
        return bookingMapper.findRatePlan(campUnitId, "weekday");
    }

    private boolean isHoliday(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY;
    }

    @Override
    @Transactional
    public void updateBookingNote(BookingNoteUpdateRequest request, Integer actorUserId, String actorName) {
        campAccessService.requireRole(actorUserId);
        if (bookingMapper.updateBookingNote(request.getBookingId(), normalizeNote(request.getAdminNote())) != 1) {
            throw new IllegalArgumentException("找不到訂單資料");
        }
        appendTimeline(request.getBookingId(), "note_updated", "更新交接備註", actorUserId, actorName, "更新內部交接備註");
    }

    private String generateBookingNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "BK" + LocalDate.now().toString().replace("-", "") + suffix;
    }

    private boolean isValidStatus(String status) {
        return "pending".equals(status)
                || "confirmed".equals(status)
                || "cancelled".equals(status)
                || "completed".equals(status);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizePhone(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized.replaceAll("\\D", "");
    }

    private String maskPhone(String value) {
        String normalized = normalizePhone(value);
        if (normalized.length() < 7) {
            return normalized;
        }
        return normalized.substring(0, 4) + "****" + normalized.substring(normalized.length() - 3);
    }

    private String normalizeNote(String value) {
        String normalized = normalize(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private int compareMoney(BigDecimal left, BigDecimal right) {
        BigDecimal safeLeft = left == null ? BigDecimal.ZERO : left;
        BigDecimal safeRight = right == null ? BigDecimal.ZERO : right;
        return safeLeft.compareTo(safeRight);
    }

    private boolean sameText(String left, String right) {
        String normalizedLeft = normalizeNote(left);
        String normalizedRight = normalizeNote(right);
        if (normalizedLeft == null) {
            return normalizedRight == null;
        }
        return normalizedLeft.equals(normalizedRight);
    }

    private String translateStatus(String status) {
        if ("pending".equals(status)) return "待確認";
        if ("confirmed".equals(status)) return "已確認";
        if ("cancelled".equals(status)) return "已取消";
        if ("completed".equals(status)) return "已完成";
        return status;
    }

    private String buildSubmittedDetail(Booking booking, BookingGuest bookingGuest) {
        return String.format(
                "由前台送出預約，聯絡人 %s，入住 %s 到 %s，共 %d 晚。",
                bookingGuest.getContactName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getNights()
        );
    }

    private void appendTimeline(
            Integer bookingId,
            String actionType,
            String actionLabel,
            Integer actorUserId,
            String actorName,
            String detail
    ) {
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(bookingId);
        timeline.setActionType(actionType);
        timeline.setActionLabel(actionLabel);
        timeline.setActorUserId(actorUserId);
        timeline.setActorName(actorName == null ? "" : actorName);
        timeline.setDetail(detail);
        timeline.setCreatedTime(LocalDateTime.now());
        bookingMapper.insertBookingTimeline(timeline);
    }

    private void validateBookingRequest(LocalDate checkInDate, LocalDate checkOutDate) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("離營日必須晚於入住日");
        }
    }

    private CampUnit requireBookableUnit(Integer campUnitId) {
        CampUnit unit = bookingMapper.findActiveUnitById(campUnitId);
        if (unit == null) {
            throw new IllegalArgumentException("找不到可預約的營位方案");
        }
        return unit;
    }

    private void validateGuestAndVehicleCount(CampUnit unit, Integer guestCount, Integer vehicleCount) {
        if (guestCount != null && unit.getMaxGuests() != null && guestCount > unit.getMaxGuests()) {
            throw new IllegalArgumentException("入住人數超過此營位上限");
        }
        if (vehicleCount != null && unit.getMaxVehicles() != null && vehicleCount > unit.getMaxVehicles()) {
            throw new IllegalArgumentException("車輛數超過此營位上限");
        }
    }

    private Map<LocalDate, Availability> toAvailabilityMap(List<Availability> availabilityList) {
        Map<LocalDate, Availability> availabilityMap = new HashMap<>();
        for (Availability availability : availabilityList) {
            availabilityMap.put(availability.getAvailableDate(), availability);
        }
        return availabilityMap;
    }

    private BigDecimal resolveBookingItemUnitPrice(BookingQuoteResponse quote) {
        if (quote.getNightsDetail() == null || quote.getNightsDetail().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return quote.getNightsDetail().get(0).getNightlyPrice();
    }

}
