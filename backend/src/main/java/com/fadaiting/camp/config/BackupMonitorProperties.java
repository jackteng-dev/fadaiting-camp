package com.fadaiting.camp.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix="backup.monitor")
public class BackupMonitorProperties {
    private String journalPath = "";
    @Pattern(regexp="[a-z0-9][a-z0-9-]{0,39}") private String sourceKey = "primary";
    @Min(60) @Max(31536000) private long maxBackupAgeSeconds = 86400;
    @Min(60) @Max(31536000) private long maxRestoreAgeSeconds = 604800;
    @Min(60) @Max(86400) private long runTimeoutSeconds = 3600;
    @Min(1) @Max(10000) private int recordLimit = 2000;
}
