package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class AdminOperationLogQueryDto {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String username;
    private String actionType;
    private Boolean success;
    private String requestPath;
    private String startDate;
    private String endDate;
}
