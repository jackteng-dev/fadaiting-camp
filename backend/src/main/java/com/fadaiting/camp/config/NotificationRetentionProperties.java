package com.fadaiting.camp.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix="notification.retention")
public class NotificationRetentionProperties {
    @Min(30) @Max(3650) private int previewDays = 180;
}
