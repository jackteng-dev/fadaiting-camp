package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class NotificationAdminQueryDto {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String bookingNo;
    private String eventType;
    private String channel;
    private String status;
}
