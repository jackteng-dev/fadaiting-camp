package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CampSite {
    private Integer id;
    private String name;
    private String slug;
    private String shortDescription;
    private String fullDescription;
    private String addressLine;
    private String city;
    private String county;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String facebookUrl;
    private String checkInTime;
    private String checkOutTime;
    private String bookingNotice;
    private String refundPolicy;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
