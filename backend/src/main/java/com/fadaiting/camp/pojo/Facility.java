package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Facility {
    private Integer id;
    private Integer campSiteId;
    private String name;
    private String icon;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
