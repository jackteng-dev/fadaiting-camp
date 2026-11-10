package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class PaymentAdminQueryDto {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String bookingNo;
    private String provider;
    private String status;
    private String merchantOrderNo;
    private String startDate;
    private String endDate;
}
