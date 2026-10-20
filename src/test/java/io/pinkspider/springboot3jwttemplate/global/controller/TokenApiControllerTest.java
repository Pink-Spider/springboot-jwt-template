package io.pinkspider.springboot3jwttemplate.global.controller;

import io.pinkspider.springboot3jwttemplate.global.config.jwt.JwtFactory;
import io.pinkspider.springboot3jwttemplate.global.config.jwt.JwtProperties;
import io.pinkspider.springboot3jwttemplate.global.config.jwt.TokenCookies;
import io.pinkspider.springboot3jwttemplate.global.config.jwt.TokenType;
import io.pinkspider.springboot3jwttemplate.global.domain.RefreshToken;
import io.pinkspider.springboot3jwttemplate.global.domain.User;
import io.pinkspider.springboot3jwttemplate.global.repository.RefreshTokenRepository;
import io.pinkspider.springboot3jwttemplate.global.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TokenApiControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    public void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DisplayName("createNewAccessToken: refresh token 쿠키로 새로운 액세스 토큰 쿠키를 발급한다.")
    @Test
    public void createNewAccessToken() throws Exception {
        // given
        final String url = "/api/token";

        User testUser = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        String refreshToken = JwtFactory.builder()
                .claims(Map.of("id", testUser.getId()))
                .tokenType(TokenType.REFRESH)
                .build()
                .createToken(jwtProperties);

        refreshTokenRepository.save(new RefreshToken(testUser.getId(), refreshToken));

        // when
        ResultActions resultActions = mockMvc.perform(post(url)
                .cookie(new Cookie(TokenCookies.REFRESH_TOKEN, refreshToken)));

        // then
        resultActions
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists(TokenCookies.ACCESS_TOKEN))
                .andExpect(cookie().httpOnly(TokenCookies.ACCESS_TOKEN, true))
                .andExpect(cookie().secure(TokenCookies.ACCESS_TOKEN, true))
                .andExpect(cookie().path(TokenCookies.ACCESS_TOKEN, "/"));
    }

    @DisplayName("createNewAccessToken: refresh token 쿠키가 없으면 401을 반환한다.")
    @Test
    public void createNewAccessToken_withoutCookie() throws Exception {
        mockMvc.perform(post("/api/token"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist(TokenCookies.ACCESS_TOKEN));
    }

    @DisplayName("createNewAccessToken: 저장되지 않은 refresh token 이면 401을 반환한다.")
    @Test
    public void createNewAccessToken_unknownToken() throws Exception {
        String refreshToken = JwtFactory.builder()
                .tokenType(TokenType.REFRESH)
                .build()
                .createToken(jwtProperties);

        mockMvc.perform(post("/api/token")
                        .cookie(new Cookie(TokenCookies.REFRESH_TOKEN, refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("createNewAccessToken: access token 으로는 재발급할 수 없다.")
    @Test
    public void createNewAccessToken_withAccessToken() throws Exception {
        // given: DB 에 저장돼 있어도 token_type 이 access 면 거부해야 한다.
        User testUser = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        String accessToken = JwtFactory.builder()
                .claims(Map.of("id", testUser.getId()))
                .tokenType(TokenType.ACCESS)
                .build()
                .createToken(jwtProperties);

        refreshTokenRepository.save(new RefreshToken(testUser.getId(), accessToken));

        // when & then
        mockMvc.perform(post("/api/token")
                        .cookie(new Cookie(TokenCookies.REFRESH_TOKEN, accessToken)))
                .andExpect(status().isUnauthorized());
    }
}
