package com.fadaiting.camp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentAdminRowDto {
    private Integer id;
    private Integer bookingId;
    private String bookingNo;
    private String bookingStatus;
    private String contactName;
    private String contactPhone;
    private String provider;
    private String paymentMethod;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String merchantOrderNo;
    private String providerTransactionId;
    private String paymentNo;
    private String paymentUrl;
    private String callbackPayload;
    private LocalDateTime paidTime;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
