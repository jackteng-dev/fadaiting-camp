package com.fadaiting.camp.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtToken {
    private String refreshToken;
    private String accessToken;

}
