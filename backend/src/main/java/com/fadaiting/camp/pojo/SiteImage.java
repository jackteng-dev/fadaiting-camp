package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteImage {
    private Integer id;
    private Integer campSiteId;
    private Integer zoneId;
    private String zoneName;
    private String title;
    private String imageUrl;
    private String altText;
    private Integer sortOrder;
    private Boolean isCover;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
