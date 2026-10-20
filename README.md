# springboot3_jwt_template

Spring Boot 4 기반 JWT 인증 템플릿 프로젝트입니다. Access Token / Refresh Token 발급과 검증을 구현합니다.

## 브랜치

| 브랜치 | Spring Boot | Java | Gradle | jjwt |
| --- | --- | --- | --- | --- |
| `main` | 4.1.1 | 25 | 9.8.2 | 0.13.0 |
| `spring-boot-3x` | 3.0.2 | 17 | 7.6.1 | 0.9.1 |

## 기술 스택 (main)

- Spring Web MVC, Spring Security 7, Spring Data JPA (Hibernate 7)
- H2 (in-memory)
- jjwt 0.13.0 (HS256)
- Lombok

## 요구 사항

- JDK 17 이상으로 Gradle 실행 (JDK 25 toolchain은 Gradle이 자동으로 찾거나 내려받습니다)

## 실행 방법

```bash
# local 프로필로 실행 (H2, JWT 설정 포함)
./gradlew bootRun --args='--spring.profiles.active=local'

# 빌드 (컴파일 + 테스트 + 패키징)
./gradlew build
```

- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:testdb`, 사용자 `sa`)

## 설정

`src/main/resources/application-local.yml`

```yaml
jwt:
  issuer: ceo@pinkspider.io
  secret_key: <HMAC 서명 키, UTF-8 기준 32바이트 이상>
```

## 인증

`/login`, `/signup`, `/user`, `/api/token` 을 제외한 모든 요청은 Access Token이 필요합니다.

```
Authorization: Bearer <access token>
```

토큰이 없거나 만료·위조된 경우 `401 Unauthorized` 를 반환합니다. 세션과 CSRF는 사용하지 않습니다(stateless).

## API

### `POST /api/token` — Refresh Token으로 새 Access Token 발급

```json
// Request
{ "refreshToken": "<refresh token>" }

// Response 201
{ "accessToken": "<access token, 2시간 유효>" }
```

## 테스트

```bash
./gradlew test                                          # 전체
./gradlew test --tests '*TokenProviderTest'             # 특정 클래스
./gradlew test --tests '*TokenProviderTest.validToken'  # 특정 메서드
```

## 프로젝트 구조

```
src/main/java/io/pinkspider/springboot3jwttemplate/global
├── config        # WebSecurityConfig, TokenAuthenticationFilter
│   └── jwt       # JwtProperties, TokenProvider
├── controller    # TokenApiController
├── domain        # User, RefreshToken
├── dto
├── repository
└── service       # TokenService, RefreshTokenService, UserService, UserDetailService
```

## TODO

- 회원가입 / 로그인 API (`/signup`, `/login`은 허용 경로로 열려 있으나 컨트롤러 미구현)

## 라이선스

[MIT License](LICENSE) © 2023 pink-spider
