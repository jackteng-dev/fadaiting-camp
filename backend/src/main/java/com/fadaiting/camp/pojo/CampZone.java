package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CampZone {
    private Integer id;
    private Integer campSiteId;
    private String name;
    private String slug;
    private String zoneType;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
