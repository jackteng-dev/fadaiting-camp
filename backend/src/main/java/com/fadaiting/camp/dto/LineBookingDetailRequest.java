package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class LineBookingDetailRequest {
    @NotBlank(message = "idToken 不得為空") @Size(max = 8192)
    private String idToken;

    @NotNull(message = "bookingId 不得為空")
    private Integer bookingId;
}
