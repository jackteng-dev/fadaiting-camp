package com.fadaiting.camp.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingItem {
    private Integer id;
    private Integer bookingId;
    private Integer campUnitId;
    private Integer ratePlanId;
    private Integer rentalItemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Integer nights;
    private BigDecimal lineTotal;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
