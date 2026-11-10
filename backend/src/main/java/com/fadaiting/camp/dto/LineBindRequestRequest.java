package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class LineBindRequestRequest {
    @NotBlank(message = "idToken 不得為空") @Size(max = 8192)
    private String idToken;
    @NotBlank(message = "訂單編號不得為空")
    private String bookingNo;

    @NotBlank(message = "聯絡電話不得為空")
    private String contactPhone;
}
