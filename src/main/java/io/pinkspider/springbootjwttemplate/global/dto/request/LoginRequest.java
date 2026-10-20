package io.pinkspider.springbootjwttemplate.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LoginRequest {
    // 로그인은 형식·길이 정책을 노출하지 않도록 빈 값만 검사한다.
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
