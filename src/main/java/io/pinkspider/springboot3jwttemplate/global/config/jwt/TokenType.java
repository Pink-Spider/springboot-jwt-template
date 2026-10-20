package io.pinkspider.springboot3jwttemplate.global.config.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// access/refresh token 은 같은 키로 서명되므로 claim 으로 용도를 구분해서
// refresh token 이 access token 으로 쓰이는 것(또는 그 반대)을 막는다.
@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("access"),
    REFRESH("refresh");

    public static final String CLAIM_NAME = "token_type";

    private final String value;
}
