package com.fadaiting.camp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentReconciliationRequest {
    @NotBlank @Pattern(regexp = "accept|refund_required|sync_ledger") private String resolution;
    @NotNull @PositiveOrZero private Long expectedVersion;
    @NotNull @PositiveOrZero private Long expectedBookingVersion;
    @NotBlank @Size(max = 1000) private String note;
    @NotBlank @Size(max = 100) private String externalReference;
}
