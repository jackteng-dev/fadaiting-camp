package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class NotificationReminderCreateRequest {
    private String targetDate;
    private Integer daysBefore = 3;
}
