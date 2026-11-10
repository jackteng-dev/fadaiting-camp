package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class StaffAccountUpdateRequest {
    @NotBlank
    @Pattern(regexp = "ADMIN|STAFF", message = "僅能選擇營區管理員或現場人員")
    private String campRole;
    @NotNull(message = "請選擇帳號狀態")
    private Boolean enabled;
    @NotNull(message = "請重新載入帳號資料")
    @PositiveOrZero
    private Long expectedVersion;
}
