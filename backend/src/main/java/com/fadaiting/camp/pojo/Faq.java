package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Faq {
    private Integer id;
    private Integer campSiteId;
    private String question;
    private String answer;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
