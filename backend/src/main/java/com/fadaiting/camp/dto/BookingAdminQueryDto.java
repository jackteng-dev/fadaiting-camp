package com.fadaiting.camp.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingAdminQueryDto {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String bookingNo;
    private String status;
    private String contactName;
    private LocalDate startDate;
    private LocalDate endDate;
}
