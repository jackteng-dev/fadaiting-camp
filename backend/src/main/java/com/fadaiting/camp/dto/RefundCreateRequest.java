package com.fadaiting.camp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RefundCreateRequest {
    @NotNull @Positive private Integer paymentRecordId;
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) private BigDecimal amount;
    @NotBlank @Size(max = 1000) private String reason;
    @NotNull private UUID idempotencyKey;
    @NotNull @PositiveOrZero private Long expectedVersion;
}
