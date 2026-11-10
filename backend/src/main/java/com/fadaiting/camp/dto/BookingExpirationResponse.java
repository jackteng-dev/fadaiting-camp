package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class BookingExpirationResponse {
    private int expiredCount;
    private int skippedCount;
    private int failedCount;
}
