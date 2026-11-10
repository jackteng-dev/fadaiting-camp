package com.fadaiting.camp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRentalItemRequest {
    @NotNull(message = "租借品 ID 不可空白")
    private Integer rentalItemId;

    @NotNull(message = "租借數量不可空白")
    @Min(value = 0, message = "租借數量不可小於 0")
    private Integer quantity;
}
