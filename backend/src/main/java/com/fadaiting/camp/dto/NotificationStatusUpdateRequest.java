package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Data
public class NotificationStatusUpdateRequest {
    @NotNull(message = "缺少通知紀錄編號")
    private Integer notificationId;

    @NotBlank(message = "缺少通知狀態")
    private String status;

    @NotNull(message = "缺少通知版本") @Min(0)
    private Long expectedVersion;
    @NotBlank(message = "請填操作原因") @Size(max=300)
    private String reason;
}
