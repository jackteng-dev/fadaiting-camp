package com.fadaiting.camp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
public class PublicPaymentCreateResponse {
    private Integer paymentRecordId;
    private String provider;
    private String paymentMethod;
    private String status;
    private String merchantOrderNo;
    private BigDecimal amount;
    private String currency;
    private String paymentUrl;
    private String redirectMethod;
    private Map<String, String> formFields;
    private String message;
}
