package com.fadaiting.camp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookingAdminUpdateRequest {
    @NotNull(message = "缺少訂單編號")
    private Integer bookingId;

    @NotNull(message = "缺少操作版本")
    @jakarta.validation.constraints.PositiveOrZero
    private Long expectedVersion;

    @NotNull(message = "請填寫已收金額")
    @DecimalMin(value = "0.00", message = "已收金額不可小於 0")
    private BigDecimal paidAmount;

    @NotNull(message = "請填寫調整金額")
    private BigDecimal adjustmentAmount;

    private String adminNote;

    private String confirmNote;

    private String cancelReason;
}
