package com.fadaiting.camp.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RentalItem {
    private Integer id;
    private Integer campSiteId;
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private BigDecimal unitPrice;
    private Integer totalQuantity;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
