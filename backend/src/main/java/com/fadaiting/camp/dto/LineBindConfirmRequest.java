package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class LineBindConfirmRequest {
    @NotBlank(message = "idToken 不得為空") @Size(max = 8192)
    private String idToken;
    @NotBlank(message = "bindToken 不得為空")
    private String bindToken;


}
