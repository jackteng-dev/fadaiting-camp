package com.fadaiting.camp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class BookingAdminRowDto {
    private Integer id;
    private String bookingNo;
    private String status;
    private String stayStatus;
    private OffsetDateTime paymentDueAt;
    private OffsetDateTime expiredAt;
    private OffsetDateTime checkedInAt;
    private OffsetDateTime checkedOutAt;
    private Long operationVersion;
    private String source;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String itemName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer guestCount;
    private Integer vehicleCount;
    private BigDecimal subtotalAmount;
    private BigDecimal adjustmentAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String bookingNote;
    private String adminNote;
    private String confirmNote;
    private String cancelReason;
    private String arrivalNote;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
