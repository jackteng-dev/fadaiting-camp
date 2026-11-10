package com.fadaiting.camp.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentRecord {
    private Integer id;
    private Integer bookingId;
    private String provider;
    private String paymentMethod;
    private String status;
    private Long operationVersion;
    private String reconciliationStatus;
    private String reconciliationNote;
    private String reconciliationReference;
    private Integer reconciledBy;
    private java.time.OffsetDateTime reconciledAt;
    private BigDecimal refundedAmount = BigDecimal.ZERO;
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
