package com.fadaiting.camp.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class Booking {
    private Integer id;
    private String bookingNo;
    private Integer campSiteId;
    private Integer memberUserId;
    private String status;
    private String stayStatus;
    private OffsetDateTime paymentDueAt;
    private OffsetDateTime expiredAt;
    private OffsetDateTime checkedInAt;
    private OffsetDateTime checkedOutAt;
    private Long operationVersion;
    private String source;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer guestCount;
    private Integer vehicleCount;
    private BigDecimal subtotalAmount;
    private BigDecimal adjustmentAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String note;
    private String adminNote;
    private String confirmNote;
    private String cancelReason;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
