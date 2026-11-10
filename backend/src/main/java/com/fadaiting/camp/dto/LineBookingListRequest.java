package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class LineBookingListRequest {
    @NotBlank(message = "idToken 不得為空") @Size(max = 8192)
    private String idToken;
}
