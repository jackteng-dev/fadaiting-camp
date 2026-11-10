package com.fadaiting.camp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PublicPaymentConfirmResponse {
    private Integer paymentRecordId;
    private Integer bookingId;
    private String bookingNo;
    private String provider;
    private String status;
    private String transactionId;
    private BigDecimal amount;
    private String message;
}
