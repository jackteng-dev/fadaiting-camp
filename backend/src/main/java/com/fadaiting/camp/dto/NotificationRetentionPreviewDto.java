package com.fadaiting.camp.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class NotificationRetentionPreviewDto {
    private final String policyVersion = "conservative-preview-v1";
    private final boolean dryRun = true;
    private final boolean deletionEnabled = false;
    private OffsetDateTime observedAt;
    private LocalDateTime cutoffAt;
    private String timeZone;
    private int previewDays;
    private long totalCount, candidateCount, protectedCount;
    private List<NotificationRetentionBucketDto> buckets;
}
