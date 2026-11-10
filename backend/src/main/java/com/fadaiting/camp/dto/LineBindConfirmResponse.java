package com.fadaiting.camp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LineBindConfirmResponse {
    private Integer bindingId;
    private String lineUserId;
    private String displayName;
    private String contactName;
    private String maskedContactPhone;
    private String status;
    private Integer bookingId;
    private String bookingNo;
    private LocalDateTime boundAt;
}
