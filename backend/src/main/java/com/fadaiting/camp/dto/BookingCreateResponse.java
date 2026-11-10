package com.fadaiting.camp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BookingCreateResponse {
    private String bookingNo;
    private Integer bookingId;
    private BigDecimal totalAmount;
    private String status;
    private java.time.OffsetDateTime paymentDueAt;
}
