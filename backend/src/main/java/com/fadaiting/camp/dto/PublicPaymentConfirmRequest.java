package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class PublicPaymentConfirmRequest {
    private Integer paymentRecordId;
    private String merchantOrderNo;
    private String transactionId;
}
