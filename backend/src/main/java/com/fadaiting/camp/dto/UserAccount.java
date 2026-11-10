package com.fadaiting.camp.dto;

import lombok.Data;

@Data
public class UserAccount {
    private Integer pageNum;
    private Integer pageSize;
    private String username;
    private Integer[] role;
    private String isStop;
}
