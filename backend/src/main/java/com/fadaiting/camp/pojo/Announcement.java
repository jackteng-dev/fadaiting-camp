package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Announcement {
    private Integer id;
    private Integer campSiteId;
    private String title;
    private String content;
    private LocalDateTime publishStartTime;
    private LocalDateTime publishEndTime;
    private Boolean isPinned;
    private Boolean isActive;
    private Integer createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
