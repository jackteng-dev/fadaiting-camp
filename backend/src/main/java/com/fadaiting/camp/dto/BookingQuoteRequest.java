package com.fadaiting.camp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BookingQuoteRequest {
    @NotNull(message = "請選擇營位方案")
    private Integer campUnitId;

    @NotNull(message = "請填寫入住日")
    private LocalDate checkInDate;

    @NotNull(message = "請填寫離營日")
    private LocalDate checkOutDate;

    @NotNull(message = "請填寫入住人數")
    @Min(value = 1, message = "入住人數至少 1 人")
    private Integer guestCount;

    @NotNull(message = "請填寫車輛數")
    @Min(value = 0, message = "車輛數不可小於 0")
    private Integer vehicleCount;

    private List<BookingRentalItemRequest> rentalItems;
}
