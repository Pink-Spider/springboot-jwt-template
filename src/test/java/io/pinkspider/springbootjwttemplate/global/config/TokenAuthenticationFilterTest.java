package io.pinkspider.springbootjwttemplate.global.config;

import io.pinkspider.springbootjwttemplate.global.config.jwt.JwtFactory;
import io.pinkspider.springbootjwttemplate.global.config.jwt.JwtProperties;
import io.pinkspider.springbootjwttemplate.global.config.jwt.TokenCookies;
import io.pinkspider.springbootjwttemplate.global.config.jwt.TokenType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TokenAuthenticationFilterTest {

    // 매핑된 컨트롤러가 없는 보호 경로. 인증을 통과하면 404, 실패하면 401 이 된다.
    private static final String PROTECTED_URL = "/api/protected";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @DisplayName("토큰이 없으면 보호된 경로에 401을 반환한다.")
    @Test
    void withoutToken() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("유효한 Bearer 토큰이면 인증을 통과한다.")
    @Test
    void withValidToken() throws Exception {
        String token = JwtFactory.withDefaultValues().createToken(jwtProperties);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @DisplayName("유효한 access token 쿠키면 인증을 통과한다.")
    @Test
    void withValidTokenCookie() throws Exception {
        String token = JwtFactory.withDefaultValues().createToken(jwtProperties);

        mockMvc.perform(get(PROTECTED_URL).cookie(new Cookie(TokenCookies.ACCESS_TOKEN, token)))
                .andExpect(status().isNotFound());
    }

    @DisplayName("만료된 토큰이면 401을 반환한다.")
    @Test
    void withExpiredToken() throws Exception {
        String token = JwtFactory.builder()
                .expiration(new Date(new Date().getTime() - Duration.ofDays(7).toMillis()))
                .build()
                .createToken(jwtProperties);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("refresh token 으로는 인증할 수 없다.")
    @Test
    void withRefreshToken() throws Exception {
        String refreshToken = JwtFactory.builder()
                .tokenType(TokenType.REFRESH)
                .build()
                .createToken(jwtProperties);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PROTECTED_URL).cookie(new Cookie(TokenCookies.ACCESS_TOKEN, refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Bearer 접두사가 없으면 토큰으로 인식하지 않는다.")
    @Test
    void withoutBearerPrefix() throws Exception {
        String token = JwtFactory.withDefaultValues().createToken(jwtProperties);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }
}
