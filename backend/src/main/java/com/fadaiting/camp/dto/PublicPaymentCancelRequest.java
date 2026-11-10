package com.fadaiting.camp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PublicPaymentCancelRequest {
    @NotNull(message = "缺少付款紀錄編號")
    private Integer paymentRecordId;
}
