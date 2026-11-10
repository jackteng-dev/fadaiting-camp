package com.fadaiting.camp.dto;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
@Data public class NotificationQueueStatusDto {
    private long dueCount, waitingCount, processingCount, failedCount;
    private long uncertainCount, expiredLeaseCount, overdueCount, blockedCount;
    private OffsetDateTime observedAt, oldestDueAt, oldestExpiredLeaseAt;
    private long oldestDueAgeSeconds;
    private int overdueThresholdSeconds;
    private String health;
    private List<NotificationQueueAlertDto> alerts;
    private String deliveryMode;
    private boolean workerEnabled;
}
