package io.pinkspider.springbootjwttemplate.global.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class AddUserRequest {
    @NotBlank
    @Email
    private String email;

    // BCrypt 는 72바이트까지만 사용하므로(초과 시 Spring Security 가 예외) 상한을 둔다.
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    private String auth;
}
