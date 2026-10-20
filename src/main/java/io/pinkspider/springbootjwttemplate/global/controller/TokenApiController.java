package io.pinkspider.springbootjwttemplate.global.controller;

import io.pinkspider.springbootjwttemplate.global.config.jwt.TokenCookies;
import io.pinkspider.springbootjwttemplate.global.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class TokenApiController {

    private final TokenService tokenService;

    @PostMapping(TokenCookies.REFRESH_TOKEN_PATH)
    public ResponseEntity<Void> createNewAccessToken(
            @CookieValue(name = TokenCookies.REFRESH_TOKEN, required = false) String refreshToken) {
        String newAccessToken = tokenService.createNewAccessToken(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,
                        TokenCookies.accessToken(newAccessToken, TokenService.ACCESS_TOKEN_DURATION).toString())
                .build();
    }

    // 로그아웃. refresh_token 쿠키는 Path=/api/token 이라 이 경로에서만 받을 수 있으므로
    // access token 이 만료된 상태에서도 로그아웃할 수 있다. 쿠키가 없어도 204 를 반환한다.
    @DeleteMapping(TokenCookies.REFRESH_TOKEN_PATH)
    public ResponseEntity<Void> logout(
            @CookieValue(name = TokenCookies.REFRESH_TOKEN, required = false) String refreshToken) {
        tokenService.deleteRefreshToken(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, TokenCookies.expireAccessToken().toString())
                .header(HttpHeaders.SET_COOKIE, TokenCookies.expireRefreshToken().toString())
                .build();
    }
}
