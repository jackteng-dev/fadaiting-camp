package com.fadaiting.camp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RefundActionRequest {
    @NotBlank @Pattern(regexp = "approve|reject|complete_manual|simulate") private String action;
    @NotNull @PositiveOrZero private Long expectedVersion;
    @NotBlank @Size(max = 1000) private String note;
    @Size(max = 100) private String externalReference;
}
