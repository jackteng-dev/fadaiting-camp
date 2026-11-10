package com.fadaiting.camp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BookingQuoteResponse {
    private Integer campUnitId;
    private String campUnitName;
    private boolean available;
    private Integer nights;
    private Integer guestCount;
    private Integer vehicleCount;
    private BigDecimal siteAmount;
    private BigDecimal rentalAmount;
    private BigDecimal totalAmount;
    private String message;
    private List<BookingQuoteNightDto> nightsDetail;
    private List<BookingQuoteRentalItemDto> rentalItems;
}
