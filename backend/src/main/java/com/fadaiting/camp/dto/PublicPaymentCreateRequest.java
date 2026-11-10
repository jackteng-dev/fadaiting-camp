package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PublicPaymentCreateRequest {
    @NotNull(message = "缺少訂單編號")
    private Integer bookingId;

    @NotBlank(message = "請選擇付款服務")
    private String provider;

    @NotBlank(message = "請選擇付款方式")
    private String paymentMethod;
}
