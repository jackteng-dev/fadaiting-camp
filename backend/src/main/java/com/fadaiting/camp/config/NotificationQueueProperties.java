package com.fadaiting.camp.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;
@Data @Validated @ConfigurationProperties(prefix="notification.queue")
public class NotificationQueueProperties {
    private boolean workerEnabled = false;
    @Pattern(regexp="disabled|simulate|smtp") private String deliveryMode = "disabled";
    @Min(1) @Max(100) private int batchSize = 20;
    @Min(1) @Max(10) private int maxAttempts = 5;
    @Min(1) @Max(3600) private int retryBaseSeconds = 60;
    @Min(1) @Max(86400) private int retryMaxSeconds = 3600;
    @Min(60) @Max(3600) private int leaseSeconds = 120;
    @Min(60) @Max(86400) private int overdueThresholdSeconds = 900;
    public long retryDelay(int attempt) { return Math.min(retryMaxSeconds, (long) retryBaseSeconds << Math.min(Math.max(attempt-1,0),20)); }
}
