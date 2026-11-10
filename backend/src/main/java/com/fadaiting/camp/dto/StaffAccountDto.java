package com.fadaiting.camp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StaffAccountDto {
    private Integer id;
    private String username;
    private String firstName;
    private String lastName;
    private String campRole;
    private Boolean enabled;
    private Long authVersion;
    private LocalDateTime createdTime;
    private LocalDateTime lastLoginTime;
}
