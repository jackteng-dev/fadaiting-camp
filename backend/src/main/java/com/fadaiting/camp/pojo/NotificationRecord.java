package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class NotificationRecord {
    private boolean queueManaged;
    private String eventKey, deliveryMode, errorCode;
    private Integer attemptCount, attemptLimit;
    private Long operationVersion;
    private OffsetDateTime nextAttemptAt, leaseUntil;
    private UUID leaseToken;
    private Integer id;
    private Integer bookingId;
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
