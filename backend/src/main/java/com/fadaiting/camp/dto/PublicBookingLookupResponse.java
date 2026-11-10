package com.fadaiting.camp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PublicBookingLookupResponse {
    private Integer bookingId;
    private String bookingNo;
    private String status;
    private String stayStatus;
    private OffsetDateTime paymentDueAt;
    private OffsetDateTime expiredAt;
    private OffsetDateTime checkedInAt;
    private OffsetDateTime checkedOutAt;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer guestCount;
    private Integer vehicleCount;
    private BigDecimal subtotalAmount;
    private BigDecimal adjustmentAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundedAmount;
    private String contactName;
    private String maskedContactPhone;
    private LocalDateTime createdTime;
    private List<Item> items;
    private List<Payment> payments;
    private List<Timeline> timeline;

    @Data
    public static class Item {
        private String itemName;
        private Integer quantity;
        private Integer nights;
        private BigDecimal lineTotal;
    }

    @Data
    public static class Payment {
        private BigDecimal refundedAmount;
        private String provider;
        private String paymentMethod;
        private String status;
        private BigDecimal amount;
        private String currency;
        private LocalDateTime paidTime;
        private LocalDateTime createdTime;
    }

    @Data
    public static class Timeline {
        private String actionType;
        private String actionLabel;
        private String detail;
        private LocalDateTime createdTime;
    }
}
