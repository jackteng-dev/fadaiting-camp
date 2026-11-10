package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.NotificationAdminQueryDto;
import com.fadaiting.camp.dto.NotificationAdminRowDto;
import com.fadaiting.camp.dto.NotificationSendQueuedRequest;
import com.fadaiting.camp.dto.NotificationSendResultDto;
import com.fadaiting.camp.dto.NotificationReminderCreateRequest;
import com.fadaiting.camp.dto.NotificationStatusUpdateRequest;
import com.fadaiting.camp.pojo.Booking;
import com.fadaiting.camp.pojo.BookingGuest;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.PaymentRecord;

public interface NotificationService {
    PageBean<NotificationAdminRowDto> getAdminNotifications(NotificationAdminQueryDto queryDto);

    void updateAdminNotificationStatus(NotificationStatusUpdateRequest request, Integer actorUserId, String actorName);

    NotificationSendResultDto sendQueuedEmailNotifications(NotificationSendQueuedRequest request, Integer actorUserId, String actorName);

    NotificationSendResultDto queueDepartureReminders(NotificationReminderCreateRequest request, Integer actorUserId, String actorName);

    void queueBookingSubmittedNotification(Booking booking, BookingGuest guest);

    void queuePaymentConfirmedNotification(Booking booking, BookingGuest guest, PaymentRecord paymentRecord);
}
