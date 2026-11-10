package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class BookingStatusUpdateRequest {
    @NotNull(message = "缺少訂單編號")
    @Positive
    private Integer bookingId;

    @NotNull @PositiveOrZero
    private Long expectedVersion;

    @NotBlank(message = "請選擇訂單狀態")
    private String status;
}
