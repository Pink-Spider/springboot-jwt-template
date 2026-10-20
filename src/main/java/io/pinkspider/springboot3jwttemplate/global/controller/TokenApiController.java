package io.pinkspider.springboot3jwttemplate.global.controller;

import io.pinkspider.springboot3jwttemplate.global.config.jwt.TokenCookies;
import io.pinkspider.springboot3jwttemplate.global.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
}
