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
@ConfigurationProperties(prefix = "booking.hold")
public class BookingHoldProperties {
    @Min(1) @Max(10080)
    private int minutes = 30;
    @Min(1) @Max(500)
    private int batchSize = 100;
    @Min(1000) @Max(3600000)
    private long sweepDelayMs = 60000;
    private boolean enabled = true;
}
