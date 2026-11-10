package com.fadaiting.camp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LineBindRequestResponse {
    private Integer bindingId;
    private String bindToken;
    private LocalDateTime expiresAt;
    private Integer bookingId;
    private String bookingNo;
    private String contactName;
    private String maskedContactPhone;
}
