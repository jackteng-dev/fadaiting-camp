package com.fadaiting.camp.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Availability {
    private Integer id;
    private Integer campSiteId;
    private Integer campUnitId;
    private LocalDate availableDate;
    private Integer totalQuantity;
    private Integer reservedQuantity;
    private Integer blockedQuantity;
    private BigDecimal priceOverride;
    private Boolean isClosed;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
