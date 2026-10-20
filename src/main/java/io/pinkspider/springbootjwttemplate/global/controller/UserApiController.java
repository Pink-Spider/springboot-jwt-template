package io.pinkspider.springbootjwttemplate.global.controller;

import io.pinkspider.springbootjwttemplate.global.config.jwt.TokenCookies;
import io.pinkspider.springbootjwttemplate.global.domain.User;
import io.pinkspider.springbootjwttemplate.global.dto.request.AddUserRequest;
import io.pinkspider.springbootjwttemplate.global.dto.request.LoginRequest;
import io.pinkspider.springbootjwttemplate.global.service.TokenService;
import io.pinkspider.springbootjwttemplate.global.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class UserApiController {

    private final UserService userService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody AddUserRequest request) {
        userService.save(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword()));
        User user = (User) authentication.getPrincipal();

        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,
                        TokenCookies.accessToken(accessToken, TokenService.ACCESS_TOKEN_DURATION).toString())
                .header(HttpHeaders.SET_COOKIE,
                        TokenCookies.refreshToken(refreshToken, TokenService.REFRESH_TOKEN_DURATION).toString())
                .build();
    }
}
