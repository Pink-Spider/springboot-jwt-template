package io.pinkspider.springboot3jwttemplate.global.controller;

import io.pinkspider.springboot3jwttemplate.global.config.jwt.TokenCookies;
import io.pinkspider.springboot3jwttemplate.global.config.jwt.TokenProvider;
import io.pinkspider.springboot3jwttemplate.global.config.jwt.TokenType;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserApiControllerTest {

    private static final String EMAIL = "signup@gmail.com";
    private static final String PASSWORD = "password1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @BeforeEach
    public void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DisplayName("signup: 회원가입하면 비밀번호를 암호화해 저장한다.")
    @Test
    public void signup() throws Exception {
        // when
        mockMvc.perform(post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isCreated());

        // then
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(bCryptPasswordEncoder.matches(PASSWORD, user.getPassword())).isTrue();
    }

    @DisplayName("signup: 이미 가입된 이메일이면 400을 반환한다.")
    @Test
    public void signup_duplicatedEmail() throws Exception {
        // given
        saveUser();

        // when & then
        mockMvc.perform(post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("login: 로그인하면 access/refresh token 을 HttpOnly 쿠키로 반환하고 refresh token 을 저장한다.")
    @Test
    public void login() throws Exception {
        // given
        User user = saveUser();

        // when
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().httpOnly(TokenCookies.ACCESS_TOKEN, true))
                .andExpect(cookie().secure(TokenCookies.ACCESS_TOKEN, true))
                .andExpect(cookie().sameSite(TokenCookies.ACCESS_TOKEN, "Lax"))
                .andExpect(cookie().path(TokenCookies.ACCESS_TOKEN, "/"))
                .andExpect(cookie().httpOnly(TokenCookies.REFRESH_TOKEN, true))
                .andExpect(cookie().secure(TokenCookies.REFRESH_TOKEN, true))
                .andExpect(cookie().sameSite(TokenCookies.REFRESH_TOKEN, "Lax"))
                .andExpect(cookie().path(TokenCookies.REFRESH_TOKEN, TokenCookies.REFRESH_TOKEN_PATH))
                .andReturn();

        // then
        Cookie accessToken = result.getResponse().getCookie(TokenCookies.ACCESS_TOKEN);
        Cookie refreshToken = result.getResponse().getCookie(TokenCookies.REFRESH_TOKEN);

        assertThat(tokenProvider.getUserId(accessToken.getValue())).isEqualTo(user.getId());
        assertThat(tokenProvider.validToken(accessToken.getValue(), TokenType.ACCESS)).isTrue();
        assertThat(tokenProvider.validToken(refreshToken.getValue(), TokenType.REFRESH)).isTrue();
        assertThat(refreshTokenRepository.findByUserId(user.getId()).orElseThrow().getRefreshToken())
                .isEqualTo(refreshToken.getValue());
    }

    @DisplayName("login: 비밀번호가 틀리면 401을 반환하고 쿠키를 내려주지 않는다.")
    @Test
    public void login_wrongPassword() throws Exception {
        // given
        saveUser();

        // when & then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist(TokenCookies.ACCESS_TOKEN))
                .andExpect(cookie().doesNotExist(TokenCookies.REFRESH_TOKEN));
    }

    @DisplayName("login: 가입되지 않은 이메일이면 401을 반환한다.")
    @Test
    public void login_unknownEmail() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("unknown@gmail.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .email(EMAIL)
                .password(bCryptPasswordEncoder.encode(PASSWORD))
                .build());
    }

    private String body(String email, String password) {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }
}
