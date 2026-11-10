package com.fadaiting.camp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ManualPaymentCreateRequest {
    @NotNull(message = "缺少訂單編號")
    private Integer bookingId;

    @NotNull(message = "請填寫付款金額")
    @DecimalMin(value = "1.00", message = "付款金額需大於 0")
    private BigDecimal amount;

    private String paymentMethod;

    private String note;
}
