package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingGuest {
    private Integer id;
    private Integer bookingId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String arrivalNote;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
