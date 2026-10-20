package io.pinkspider.springboot3jwttemplate.global.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LoginRequest {
    private String email;
    private String password;
}
