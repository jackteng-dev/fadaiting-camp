package com.fadaiting.camp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAdminSummaryDto {
    private String status;
    private Long recordCount;
    private BigDecimal totalAmount;
}
