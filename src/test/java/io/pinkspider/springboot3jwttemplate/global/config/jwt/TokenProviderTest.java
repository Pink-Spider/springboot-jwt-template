package io.pinkspider.springboot3jwttemplate.global.config.jwt;

import io.jsonwebtoken.Jwts;
import io.pinkspider.springboot3jwttemplate.global.domain.User;
import io.pinkspider.springboot3jwttemplate.global.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@ActiveProfiles("local")
class TokenProviderTest {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @DisplayName("generateToken(): 유저 정보와 만료 기간을 전달해 토큰을 만들 수 있다.")
    @Test
    void generateToken() {
        // given
        User testUser = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        // when
        String token = tokenProvider.generateToken(testUser, Duration.ofDays(14), TokenType.ACCESS);

        // then
        Long userId = Jwts.parser()
                .verifyWith(jwtProperties.signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("id", Long.class);

        assertThat(userId).isEqualTo(testUser.getId());
    }

    @DisplayName("validToken(): 만료된 토큰인 경우에 유효성 검증에 실패한다.")
    @Test
    void validToken_invalidToken() {
        // given
        String token = JwtFactory.builder()
                .expiration(new Date(new Date().getTime() - Duration.ofDays(7).toMillis()))
                .build()
                .createToken(jwtProperties);

        // when
        boolean result = tokenProvider.validToken(token, TokenType.ACCESS);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("validToken(): 토큰 용도(token_type)가 다르면 유효성 검증에 실패한다.")
    @Test
    void validToken_wrongTokenType() {
        // given
        String refreshToken = JwtFactory.builder()
                .tokenType(TokenType.REFRESH)
                .build()
                .createToken(jwtProperties);

        // when & then
        assertThat(tokenProvider.validToken(refreshToken, TokenType.ACCESS)).isFalse();
        assertThat(tokenProvider.validToken(refreshToken, TokenType.REFRESH)).isTrue();
    }

    @DisplayName("validToken(): token_type 이 없는 토큰은 유효성 검증에 실패한다.")
    @Test
    void validToken_withoutTokenType() {
        // given
        String token = Jwts.builder()
                .subject("user@email.com")
                .expiration(new Date(new Date().getTime() + Duration.ofDays(1).toMillis()))
                .signWith(jwtProperties.signingKey(), Jwts.SIG.HS256)
                .compact();

        // when & then
        assertThat(tokenProvider.validToken(token, TokenType.ACCESS)).isFalse();
        assertThat(tokenProvider.validToken(token, TokenType.REFRESH)).isFalse();
    }

    @DisplayName("validToken(): 유효한 토큰인 경우에 유효성 검증에 성공한다.")
    @Test
    void validToken() {
        // given
        String token = JwtFactory.withDefaultValues()
                .createToken(jwtProperties);

        // when
        boolean result = tokenProvider.validToken(token, TokenType.ACCESS);

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("getAuthentication(): 토큰 기반으로 인증정보를 가져올 수 있다.")
    @Test
    void getAuthentication() {
        // given
        String userEmail = "user@email.com";
        String token = JwtFactory.builder()
                .subject(userEmail)
                .build()
                .createToken(jwtProperties);

        // when
        Authentication authentication = tokenProvider.getAuthentication(token);

        // then
        assertThat(((UserDetails) authentication.getPrincipal()).getUsername()).isEqualTo(userEmail);
    }

    @DisplayName("getUserId(): 토큰으로 유저 ID를 가져올 수 있다.")
    @Test
    void getUserId() {
        // given
        Long userId = 1L;
        String token = JwtFactory.builder()
                .claims(Map.of("id", userId))
                .build()
                .createToken(jwtProperties);

        // when
        Long userIdByToken = tokenProvider.getUserId(token);

        // then
        assertThat(userIdByToken).isEqualTo(userId);
    }
}
