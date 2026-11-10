package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingTimeline {
    private Integer id;
    private Integer bookingId;
    private String actionType;
    private String actionLabel;
    private Integer actorUserId;
    private String actorName;
    private String detail;
    private LocalDateTime createdTime;
}
