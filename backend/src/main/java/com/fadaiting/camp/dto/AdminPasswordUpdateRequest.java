package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPasswordUpdateRequest {
    @NotBlank(message = "請輸入目前密碼")
    private String currentPassword;

    @NotBlank(message = "請輸入新密碼")
    @Size(min = 8, max = 24, message = "新密碼長度需為 8 到 24 碼")
    private String newPassword;

    @NotBlank(message = "請再次輸入新密碼")
    private String confirmPassword;
}
