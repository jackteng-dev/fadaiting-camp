package com.fadaiting.camp.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class BackupMonitorStatusDto {
    private OffsetDateTime observedAt;
    private boolean configured, journalAvailable, latestBackupVerified;
    private String sourceKey, health = "unknown";
    private int recordCount;
    private long maxBackupAgeSeconds, maxRestoreAgeSeconds, runTimeoutSeconds;
    private RunSummary lastBackupAttempt, lastRestoreAttempt, lastBackupSuccess, lastRestoreSuccess;
    private OffsetDateTime latestBackupVerifiedAt;
    private List<Alert> alerts = new ArrayList<>();
    public record RunSummary(String status, OffsetDateTime startedAt, OffsetDateTime finishedAt,
            String archiveSha256, String errorCode) { }
    public record Alert(String code, String severity, String message, String action) { }
}
