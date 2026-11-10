package com.fadaiting.camp.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookingQuoteRentalItemDto {
    private Integer rentalItemId;
    private String name;
    private String category;
    private String imageUrl;
    private Integer quantity;
    private Integer totalQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
