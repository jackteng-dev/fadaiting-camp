package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.dto.BookingStayUpdateRequest;
import com.fadaiting.camp.expection.BookingConflictException;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingTimeline;
import com.fadaiting.camp.service.BookingStayService;
import com.fadaiting.camp.service.CampAccessService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Set;

@Service
public class BookingStayServiceImpl implements BookingStayService {
    private final BookingMapper mapper;
    private final CampAccessService access;
    private final Clock clock;
    public BookingStayServiceImpl(BookingMapper mapper, CampAccessService access, Clock clock) {
        this.mapper = mapper; this.access = access; this.clock = clock;
    }
    private void requireStaff(Integer actorId) {
        if (!Set.of("ADMIN", "STAFF").contains(access.currentRole(actorId))) throw new AccessDeniedException("無權限登記入住或退房");
    }
    @Override
    @Transactional
    public Booking updateStay(BookingStayUpdateRequest request, Integer actorId, String actorName) {
        requireStaff(actorId);
        Booking booking = mapper.findBookingForUpdate(request.getBookingId());
        requireStaff(actorId);
        if (booking == null) throw new IllegalArgumentException("找不到訂單資料");
        if (!booking.getOperationVersion().equals(request.getExpectedVersion())) throw new BookingConflictException("訂單已更新，請重新載入後再登記");
        if (!"confirmed".equals(booking.getStatus())) throw new BookingConflictException("僅已確認的訂單可登記入住或退房");
        OffsetDateTime now = OffsetDateTime.now(clock);
        String label;
        int updated;
        if ("check_in".equals(request.getAction())) {
            if (!"not_checked_in".equals(booking.getStayStatus())) throw new BookingConflictException("此訂單已登記入住，請重新載入");
            LocalDate today = LocalDate.now(clock);
            if (today.isBefore(booking.getCheckInDate()) || !today.isBefore(booking.getCheckOutDate())) throw new BookingConflictException("入住登記需在預約入住日至離營日前一天之間（台北時間）");
            updated = mapper.checkInBooking(booking.getId(), request.getExpectedVersion(), now);
            label = "登記入住";
        } else if ("check_out".equals(request.getAction())) {
            if (!"checked_in".equals(booking.getStayStatus())) throw new BookingConflictException("需先登記入住才能退房");
            if (booking.getCheckedInAt() == null || now.isBefore(booking.getCheckedInAt())) throw new BookingConflictException("退房時間不可早於入住時間");
            updated = mapper.checkOutBooking(booking.getId(), request.getExpectedVersion(), now);
            label = "登記退房";
        } else throw new IllegalArgumentException("不支援的住宿操作");
        if (updated != 1) throw new BookingConflictException("訂單已更新，請重新載入後再登記");
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(booking.getId());
        timeline.setActionType("check_in".equals(request.getAction()) ? "checked_in" : "checked_out");
        timeline.setActionLabel(label);
        timeline.setActorUserId(actorId);
        timeline.setActorName(actorName == null ? "" : actorName);
        timeline.setDetail(label + ("check_out".equals(request.getAction()) ? "，住宿行程已完成" : "，已確認旅客抵達"));
        timeline.setCreatedTime(LocalDateTime.ofInstant(now.toInstant(), java.time.ZoneId.systemDefault()));
        mapper.insertBookingTimeline(timeline);
        return mapper.findBookingById(booking.getId());
    }
}
