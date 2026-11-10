package com.fadaiting.camp.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NotificationRetentionBucketDto {
    private String reason;
    private long count, simulatedCount, skippedCount, acknowledgedCount;
    private LocalDateTime oldestActivityAt, newestActivityAt;
}
