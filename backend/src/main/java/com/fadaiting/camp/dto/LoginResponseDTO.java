package com.fadaiting.camp.dto;

import com.fadaiting.camp.pojo.JwtToken;
import com.fadaiting.camp.pojo.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private JwtToken jwtToken;
    private User user;
}
