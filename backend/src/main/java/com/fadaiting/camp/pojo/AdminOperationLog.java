package com.fadaiting.camp.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOperationLog {
    private Integer id;
    private Integer userId;
    private String username;
    private String actionType;
    private String actionLabel;
    private String requestMethod;
    private String requestPath;
    private String queryString;
    private Integer statusCode;
    private Boolean success;
    private String message;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdTime;
}
