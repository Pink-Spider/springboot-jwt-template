package io.pinkspider.springboot3jwttemplate.global.service;

import io.pinkspider.springboot3jwttemplate.global.config.jwt.TokenProvider;
import io.pinkspider.springboot3jwttemplate.global.config.jwt.TokenType;
import io.pinkspider.springboot3jwttemplate.global.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class TokenService {

    public static final Duration ACCESS_TOKEN_DURATION = Duration.ofHours(2);
    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(14);

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public String createAccessToken(User user) {
        return tokenProvider.generateToken(user, ACCESS_TOKEN_DURATION, TokenType.ACCESS);
    }

    // 사용자당 refresh token 은 하나만 유지한다. 다시 로그인하면 이전 토큰은 무효가 된다.
    public String createRefreshToken(User user) {
        String refreshToken = tokenProvider.generateToken(user, REFRESH_TOKEN_DURATION, TokenType.REFRESH);
        refreshTokenService.saveOrUpdate(user.getId(), refreshToken);

        return refreshToken;
    }

    // 로그아웃. 만료·위조 여부와 관계없이 저장된 값과 같으면 삭제한다.
    public void deleteRefreshToken(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.delete(refreshToken);
        }
    }

    public String createNewAccessToken(String refreshToken) {
        // 토큰 유효성 검사에 실패하면 예외 발생
        if(!tokenProvider.validToken(refreshToken, TokenType.REFRESH)) {
            throw new BadCredentialsException("Unexpected token");
        }

        Long userId = refreshTokenService.findByRefreshToken(refreshToken).getUserId();
        User user = userService.findById(userId);

        return createAccessToken(user);
    }
}
