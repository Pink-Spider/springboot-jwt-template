package io.pinkspider.springboot3jwttemplate.global.config.jwt;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

// 토큰은 JS 에서 접근할 수 없도록 HttpOnly 쿠키로만 내려준다.
// SameSite=Lax 로 cross-site POST 에 쿠키가 실리지 않게 해서 CSRF 를 막는다.
public final class TokenCookies {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    // refresh token 은 재발급 API 로만 전송되도록 경로를 제한한다.
    public static final String REFRESH_TOKEN_PATH = "/api/token";

    private TokenCookies() {
    }

    public static ResponseCookie accessToken(String token, Duration maxAge) {
        return build(ACCESS_TOKEN, token, "/", maxAge);
    }

    public static ResponseCookie refreshToken(String token, Duration maxAge) {
        return build(REFRESH_TOKEN, token, REFRESH_TOKEN_PATH, maxAge);
    }

    private static ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
