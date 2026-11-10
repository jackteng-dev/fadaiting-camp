package com.fadaiting.camp.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RatePlan {
    private Integer id;
    private Integer campSiteId;
    private Integer campUnitId;
    private String name;
    private String dayType;
    private BigDecimal basePrice;
    private BigDecimal extraPersonPrice;
    private BigDecimal cleaningFee;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
