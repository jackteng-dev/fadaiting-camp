package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class NotificationSendQueuedRequest {
    private Integer limit = 20;
    private String bookingNo;
}
