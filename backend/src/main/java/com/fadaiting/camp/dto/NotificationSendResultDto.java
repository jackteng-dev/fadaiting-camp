package com.fadaiting.camp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationSendResultDto {
    private Integer createdCount = 0;
    private Integer unresolvedCount = 0;
    private Integer simulatedCount = 0;
    private Integer retryCount = 0;
    public NotificationSendResultDto(Integer processed, Integer sent, Integer failed, Integer skipped) {
        this.processedCount=processed; this.sentCount=sent; this.failedCount=failed; this.skippedCount=skipped;
    }
    private Integer processedCount;
    private Integer sentCount;
    private Integer failedCount;
    private Integer skippedCount;
}
