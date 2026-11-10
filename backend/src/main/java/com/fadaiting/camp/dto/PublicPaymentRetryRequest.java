package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublicPaymentRetryRequest {
    @NotBlank(message = "缺少訂單編號")
    private String bookingNo;

    @NotBlank(message = "請填寫聯絡電話")
    private String contactPhone;

    @NotBlank(message = "請選擇付款服務")
    private String provider;

    @NotBlank(message = "請選擇付款方式")
    private String paymentMethod;
}
