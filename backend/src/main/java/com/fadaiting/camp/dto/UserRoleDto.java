package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class UserRoleDto {
    private Integer id;
    private Integer userId;
    private Integer[] roleId;
}
