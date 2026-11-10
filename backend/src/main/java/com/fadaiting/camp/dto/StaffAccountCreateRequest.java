package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffAccountCreateRequest {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_]{8,20}$", message = "帳號需為 8 到 20 碼英數字或底線")
    private String username;
    @NotBlank
    @Size(min = 8, max = 24, message = "密碼需為 8 到 24 碼")
    private String password;
    @NotBlank(message = "請再次輸入密碼")
    private String confirmPassword;
    @NotBlank
    @Pattern(regexp = "ADMIN|STAFF", message = "僅能選擇營區管理員或現場人員")
    private String campRole;
    @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lastName;
}
