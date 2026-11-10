package com.fadaiting.camp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fadaiting.camp.dto.NotificationAdminQueryDto;
import com.fadaiting.camp.dto.NotificationAdminRowDto;
import com.fadaiting.camp.dto.NotificationReminderCreateRequest;
import com.fadaiting.camp.dto.NotificationSendQueuedRequest;
import com.fadaiting.camp.dto.NotificationSendResultDto;
import com.fadaiting.camp.dto.NotificationStatusUpdateRequest;
import com.fadaiting.camp.mapper.BookingMapper;
import com.fadaiting.camp.mapper.NotificationMapper;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingGuest;
import com.fadaiting.camp.pojo.BookingTimeline;
import com.fadaiting.camp.pojo.NotificationRecord;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.PaymentRecord;
import com.fadaiting.camp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import java.time.OffsetDateTime;
import com.fadaiting.camp.service.NotificationQueueService;
import com.fadaiting.camp.config.NotificationQueueProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private BookingMapper bookingMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired private NotificationQueueService queue;
    @Autowired private NotificationQueueProperties properties;
    @Autowired private Clock clock;

    @Override
    public PageBean<NotificationAdminRowDto> getAdminNotifications(NotificationAdminQueryDto queryDto) {
        if (queryDto.getPageNum()==null || queryDto.getPageNum()<1 || queryDto.getPageSize()==null || queryDto.getPageSize()<1 || queryDto.getPageSize()>100) throw new IllegalArgumentException("頁碼與每頁筆數無效");
        String bookingNo=normalize(queryDto.getBookingNo()), eventType=normalize(queryDto.getEventType()),
                channel=normalize(queryDto.getChannel()), status=normalize(queryDto.getStatus());
        long total=notificationMapper.countAdminNotifications(bookingNo,eventType,channel,status);
        return new PageBean<>(total,notificationMapper.findAdminNotifications(bookingNo,eventType,channel,status,
                queryDto.getPageSize(),((long)queryDto.getPageNum()-1)*queryDto.getPageSize()));
    }

    @Override
    public void updateAdminNotificationStatus(NotificationStatusUpdateRequest request, Integer actorUserId, String actorName) {
        queue.updateStatus(request,actorUserId,actorName);
    }

    @Override
    public NotificationSendResultDto sendQueuedEmailNotifications(NotificationSendQueuedRequest request, Integer actorUserId, String actorName) {
        return queue.dispatch(request,"smtp",actorUserId,actorName);
    }

    @Override
    @Transactional
    public NotificationSendResultDto queueDepartureReminders(NotificationReminderCreateRequest request, Integer actorUserId, String actorName) {
        int daysBefore = request.getDaysBefore() == null ? 3 : Math.max(0, Math.min(request.getDaysBefore(), 30));
        LocalDate targetDate = resolveReminderTargetDate(request.getTargetDate(), daysBefore);
        if (targetDate.isBefore(LocalDate.now(clock))) throw new IllegalArgumentException("不可產生過去日期的入住提醒");
        int processed = 0;
        int created = 0;
        int skipped = 0;

        for (Booking booking : bookingMapper.findActiveBookingsByCheckInDate(targetDate)) {
            processed++;
            booking = bookingMapper.findBookingForUpdate(booking.getId());
            if (booking == null || !"confirmed".equals(booking.getStatus()) || !"not_checked_in".equals(booking.getStayStatus()) || !targetDate.equals(booking.getCheckInDate())) {
                skipped++;
                continue;
            }
            BookingGuest guest = bookingMapper.findBookingGuestByBookingId(booking.getId());
            NotificationRecord record = buildGuestRecord(booking, guest);
            record.setEventType("departure_reminder");
            record.setSubject("發呆亭露營區入住前提醒");
            record.setMessage(String.format(
                    "親愛的 %s，您好，提醒您訂單 %s 將於 %s 入住、%s 退住，共 %d 晚。請留意行前用品、交通時間與營區通知；若行程需調整，請提前聯繫營區。",
                    safeName(guest),
                    booking.getBookingNo(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getNights()
            ));
            record.setPayload(toJson(Map.of(
                    "bookingNo", booking.getBookingNo(),
                    "eventType", "departure_reminder",
                    "targetDate", targetDate,
                    "daysBefore", daysBefore
            )));
            if (!enqueue(record,"booking:"+booking.getId()+":departure:"+targetDate)) { skipped++; continue; }
            appendTimeline(
                    booking.getId(),
                    actorUserId,
                    actorName,
                    String.format("已建立出發前提醒通知，狀態為「%s」。", translateStatus(record.getStatus()))
            );
            created++;
        }

        var result=new NotificationSendResultDto(processed,0,0,skipped);
        result.setCreatedCount(created);
        return result;
    }

    @Override
    @Transactional
    public void queueBookingSubmittedNotification(Booking booking, BookingGuest guest) {
        NotificationRecord record = buildGuestRecord(booking, guest);
        record.setEventType("booking_submitted");
        record.setSubject("發呆亭露營區已收到您的預約申請");
        record.setMessage(String.format(
                "親愛的 %s，您好，我們已收到您的預約申請。訂單編號 %s，入住日期 %s，退住日期 %s，共 %d 晚，總金額 NT$ %s。我們將再進行人工確認。",
                safeName(guest),
                booking.getBookingNo(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getNights(),
                booking.getTotalAmount()
        ));
        record.setPayload(toJson(Map.of(
                "bookingNo", booking.getBookingNo(),
                "eventType", "booking_submitted"
        )));
        boolean guestCreated=enqueue(record,"booking:"+booking.getId()+":submitted:guest");
        boolean adminCreated=enqueue(buildAdminBookingAlertRecord(booking,guest),"booking:"+booking.getId()+":submitted:internal");
        if (!guestCreated && !adminCreated) return;
        appendTimeline(
                booking.getId(),
                null,
                "通知系統",
                String.format("已建立預約送出通知，狀態為「%s」。", translateStatus(record.getStatus()))
        );
    }

    @Override
    @Transactional
    public void queuePaymentConfirmedNotification(Booking booking, BookingGuest guest, PaymentRecord paymentRecord) {
        NotificationRecord record = buildGuestRecord(booking, guest);
        record.setEventType("payment_confirmed");
        record.setSubject("發呆亭露營區已收到您的付款");
        record.setMessage(String.format(
                "親愛的 %s，您好，我們已收到您的付款。訂單編號 %s，本次付款方式 %s，付款金額 NT$ %s，目前已收金額 NT$ %s。",
                safeName(guest),
                booking.getBookingNo(),
                translateProvider(paymentRecord.getProvider()),
                paymentRecord.getAmount(),
                booking.getPaidAmount()
        ));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingNo", booking.getBookingNo());
        payload.put("paymentRecordId", paymentRecord.getId());
        payload.put("provider", paymentRecord.getProvider());
        payload.put("amount", paymentRecord.getAmount());
        record.setPayload(toJson(payload));
        boolean guestCreated=enqueue(record,"payment:"+paymentRecord.getId()+":confirmed:guest");
        boolean adminCreated=enqueue(buildAdminPaymentAlertRecord(booking,guest,paymentRecord),"payment:"+paymentRecord.getId()+":confirmed:internal");
        if (!guestCreated && !adminCreated) return;
        appendTimeline(
                booking.getId(),
                null,
                "通知系統",
                String.format("已建立付款成功通知，狀態為「%s」。", translateStatus(record.getStatus()))
        );
    }

    private boolean enqueue(NotificationRecord record,String key) {
        record.setQueueManaged(true); record.setEventKey(key); record.setAttemptLimit(properties.getMaxAttempts());
        if ("email".equals(record.getChannel()) && "queued".equals(record.getStatus())) record.setNextAttemptAt(OffsetDateTime.now(clock));
        return notificationMapper.insertNotification(record)==1;
    }

    private NotificationRecord buildGuestRecord(Booking booking, BookingGuest guest) {
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationRecord record = new NotificationRecord();
        record.setBookingId(booking.getId());
        record.setProvider("system");
        record.setRecipientName(guest == null ? "" : safeText(guest.getContactName()));
        record.setCreatedTime(now);
        record.setModifiedTime(now);

        String email = guest == null ? "" : safeText(guest.getContactEmail());
        if (!email.isBlank()) {
            record.setChannel("email");
            record.setRecipient(email);
            record.setStatus("queued");
        } else {
            record.setChannel("manual");
            record.setRecipient(guest == null ? "" : safeText(guest.getContactPhone()));
            record.setStatus("skipped");
            record.setErrorMessage("未提供 Email，待後續決定其他通知通道");
        }
        return record;
    }

    private NotificationRecord buildAdminBookingAlertRecord(Booking booking, BookingGuest guest) {
        NotificationRecord record = buildInternalRecord(booking, guest);
        record.setEventType("admin_new_booking");
        record.setSubject("後台新訂單提醒");
        record.setMessage(String.format(
                "有新的預約訂單待處理。訂單編號 %s，聯絡人 %s，入住 %s 到 %s，共 %d 晚，總金額 NT$ %s。",
                booking.getBookingNo(),
                safeName(guest),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getNights(),
                booking.getTotalAmount()
        ));
        record.setPayload(toJson(Map.of(
                "bookingNo", booking.getBookingNo(),
                "eventType", "admin_new_booking",
                "bookingId", booking.getId()
        )));
        return record;
    }

    private NotificationRecord buildAdminPaymentAlertRecord(Booking booking, BookingGuest guest, PaymentRecord paymentRecord) {
        NotificationRecord record = buildInternalRecord(booking, guest);
        record.setEventType("admin_payment_confirmed");
        record.setSubject("後台付款入帳提醒");
        record.setMessage(String.format(
                "訂單 %s 已收到付款。聯絡人 %s，付款方式 %s，付款金額 NT$ %s，目前已收金額 NT$ %s。",
                booking.getBookingNo(),
                safeName(guest),
                translateProvider(paymentRecord.getProvider()),
                paymentRecord.getAmount(),
                booking.getPaidAmount()
        ));
        record.setPayload(toJson(Map.of(
                "bookingNo", booking.getBookingNo(),
                "eventType", "admin_payment_confirmed",
                "bookingId", booking.getId(),
                "paymentRecordId", paymentRecord.getId()
        )));
        return record;
    }

    private NotificationRecord buildInternalRecord(Booking booking, BookingGuest guest) {
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationRecord record = new NotificationRecord();
        record.setBookingId(booking.getId());
        record.setChannel("internal");
        record.setRecipient("後台通知中心");
        record.setRecipientName(guest == null ? "" : safeText(guest.getContactName()));
        record.setStatus("queued");
        record.setProvider("system");
        record.setCreatedTime(now);
        record.setModifiedTime(now);
        return record;
    }

    private void appendTimeline(Integer bookingId, Integer actorUserId, String actorName, String detail) {
        BookingTimeline timeline = new BookingTimeline();
        timeline.setBookingId(bookingId);
        timeline.setActionType("notification_updated");
        timeline.setActionLabel("更新通知紀錄");
        timeline.setActorUserId(actorUserId);
        timeline.setActorName(actorName == null ? "" : actorName);
        timeline.setDetail(detail);
        timeline.setCreatedTime(LocalDateTime.now(clock));
        bookingMapper.insertBookingTimeline(timeline);
    }

    private String translateStatus(String status) {
        if ("queued".equals(status)) return "待發送";
        if ("sent".equals(status)) return "已發送";
        if ("failed".equals(status)) return "發送失敗";
        if ("skipped".equals(status)) return "略過";
        return status == null ? "" : status;
    }

    private LocalDate resolveReminderTargetDate(String targetDate, int daysBefore) {
        String normalizedTargetDate = safeText(targetDate);
        if (!normalizedTargetDate.isBlank()) {
            return LocalDate.parse(normalizedTargetDate);
        }
        return LocalDate.now(clock).plusDays(daysBefore);
    }

    private String translateProvider(String provider) {
        if ("linepay".equals(provider)) return "LINE Pay";
        if ("ecpay".equals(provider)) return "綠界";
        return provider == null ? "" : provider;
    }

    private String safeName(BookingGuest guest) {
        if (guest == null || safeText(guest.getContactName()).isBlank()) {
            return "旅客";
        }
        return safeText(guest.getContactName());
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        String normalized = safeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("通知內容建立失敗");
        }
    }

}
