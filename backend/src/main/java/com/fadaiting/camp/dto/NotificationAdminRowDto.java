package com.fadaiting.camp.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class NotificationAdminRowDto {
    private boolean queueManaged;
    private String eventKey, deliveryMode, errorCode;
    private Integer attemptCount, attemptLimit;
    private Long operationVersion;
    private OffsetDateTime nextAttemptAt, leaseUntil;
    private Integer id;
    private Integer bookingId;
    private String bookingNo;
    private String bookingStatus;
    private String contactName;
    private String contactPhone;
    private String channel;
    private String eventType;
    private String recipient;
    private String recipientName;
    private String subject;
    private String message;
    private String status;
    private String provider;
    private String errorMessage;
    private String payload;
    private LocalDateTime sentTime;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
