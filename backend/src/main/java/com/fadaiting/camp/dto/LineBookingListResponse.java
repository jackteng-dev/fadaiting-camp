package com.fadaiting.camp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LineBookingListResponse {
    private String lineUserId;
    private String displayName;
    private String contactName;
    private String maskedContactPhone;
    private List<Item> bookings;

    @Data
    public static class Item {
        private Integer bookingId;
        private String bookingNo;
        private String status;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private Integer nights;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private LocalDateTime createdTime;
    }
}
