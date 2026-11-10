package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.NotificationAdminQueryDto;
import com.fadaiting.camp.dto.NotificationAdminRowDto;
import com.fadaiting.camp.dto.NotificationSendQueuedRequest;
import com.fadaiting.camp.dto.NotificationSendResultDto;
import com.fadaiting.camp.dto.NotificationReminderCreateRequest;
import com.fadaiting.camp.dto.NotificationStatusUpdateRequest;
import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.NotificationService;
import jakarta.validation.Valid;
import com.fadaiting.camp.service.NotificationQueueService;
import com.fadaiting.camp.dto.NotificationQueueStatusDto;
import com.fadaiting.camp.dto.NotificationRetentionPreviewDto;
import com.fadaiting.camp.service.NotificationRetentionService;
import com.fadaiting.camp.pojo.NotificationAttempt;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
public class NotificationAdminController {
    @Autowired
    private NotificationService notificationService;
    @Autowired private NotificationQueueService queue;
    @Autowired private NotificationRetentionService retention;
    @GetMapping("/retention-preview")
    public Result<NotificationRetentionPreviewDto> retentionPreview() {
        return Result.success(retention.preview());
    }
    @GetMapping("/queue")
    public Result<NotificationQueueStatusDto> queueStatus() { return Result.success(queue.status()); }
    @GetMapping("/{id}/attempts")
    public Result<List<NotificationAttempt>> attempts(@PathVariable int id) { return Result.success(queue.attempts(id)); }
    @PostMapping("/simulate-queued")
    public Result<NotificationSendResultDto> simulate(@RequestBody(required=false) NotificationSendQueuedRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return Result.success(queue.dispatch(request==null ? new NotificationSendQueuedRequest() : request,"simulate",user.getId(),user.getUsername()));
    }


    @PostMapping("/list")
    public Result<PageBean<NotificationAdminRowDto>> list(@RequestBody NotificationAdminQueryDto queryDto) {
        return Result.success(notificationService.getAdminNotifications(queryDto));
    }

    @PostMapping("/send-queued")
    public Result<NotificationSendResultDto> sendQueued(
            @RequestBody(required = false) NotificationSendQueuedRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        NotificationSendQueuedRequest safeRequest = request == null ? new NotificationSendQueuedRequest() : request;
        return Result.success(notificationService.sendQueuedEmailNotifications(safeRequest, currentUser.getId(), currentUser.getUsername()));
    }

    @PostMapping("/departure-reminders")
    public Result<NotificationSendResultDto> createDepartureReminders(
            @RequestBody(required = false) NotificationReminderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        NotificationReminderCreateRequest safeRequest = request == null ? new NotificationReminderCreateRequest() : request;
        return Result.success(notificationService.queueDepartureReminders(safeRequest, currentUser.getId(), currentUser.getUsername()));
    }

    @PatchMapping("/status")
    public Result<Void> updateStatus(
            @RequestBody @Valid NotificationStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        notificationService.updateAdminNotificationStatus(request, currentUser.getId(), currentUser.getUsername());
        return Result.success();
    }
}
