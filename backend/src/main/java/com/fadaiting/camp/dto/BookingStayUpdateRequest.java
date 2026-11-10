package com.fadaiting.camp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BookingStayUpdateRequest {
    @NotNull @Positive
    private Integer bookingId;
    @NotBlank @Pattern(regexp = "check_in|check_out", message = "僅支援入住或退房登記")
    private String action;
    @NotNull @PositiveOrZero
    private Long expectedVersion;
}
