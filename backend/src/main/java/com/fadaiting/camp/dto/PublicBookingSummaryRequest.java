package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PublicBookingSummaryRequest {
    @NotNull(message = "缺少訂單 ID")
    private Integer bookingId;

    @NotBlank(message = "請填寫訂單編號")
    private String bookingNo;
}
