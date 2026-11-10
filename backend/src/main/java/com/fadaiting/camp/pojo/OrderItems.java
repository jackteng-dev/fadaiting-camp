package com.fadaiting.camp.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderItems {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private Integer quantity;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
}
