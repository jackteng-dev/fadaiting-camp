package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CampUnit {
    private Integer id;
    private Integer campSiteId;
    private Integer zoneId;
    private String name;
    private String unitType;
    private String description;
    private Integer maxGuests;
    private Integer maxVehicles;
    private Integer maxTents;
    private Boolean hasShelter;
    private Boolean electricityAvailable;
    private Boolean privateBathroom;
    private Integer quantity;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
