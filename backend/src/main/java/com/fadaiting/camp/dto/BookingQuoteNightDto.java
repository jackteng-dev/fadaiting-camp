package com.fadaiting.camp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class BookingQuoteNightDto {
    private LocalDate date;
    private BigDecimal nightlyPrice;
    private Integer availableQuantity;
    private boolean closed;
}
