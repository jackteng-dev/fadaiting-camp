package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentMethodConfig {
    private Integer id;
    private String provider;
    private String displayName;
    private String description;
    private String paymentMethod;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
