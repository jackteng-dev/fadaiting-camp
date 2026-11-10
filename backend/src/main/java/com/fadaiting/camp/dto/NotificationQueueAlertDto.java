package com.fadaiting.camp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationQueueAlertDto {
    private String code;
    private String severity;
    private long count;
    private String message;
    private String action;
}
