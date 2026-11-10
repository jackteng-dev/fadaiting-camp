package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublicBookingLookupRequest {
    @NotBlank(message = "請填寫訂單編號")
    private String bookingNo;

    @NotBlank(message = "請填寫聯絡電話")
    private String contactPhone;
}
