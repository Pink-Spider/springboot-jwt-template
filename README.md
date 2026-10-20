# springboot3_jwt_template

Spring Boot 4 기반 JWT 인증 템플릿 프로젝트입니다. 회원가입·로그인과 Access Token / Refresh Token 발급·검증을 HttpOnly 쿠키 기반으로 구현합니다.

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

로그인하면 Access Token과 Refresh Token을 **HttpOnly 쿠키**로 내려줍니다. 응답 본문에는 토큰이 없으며, JavaScript에서 토큰에 접근할 수 없습니다.

| 쿠키 | 유효 기간 | Path | 속성 |
| --- | --- | --- | --- |
| `access_token` | 2시간 | `/` | `HttpOnly; Secure; SameSite=Lax` |
| `refresh_token` | 14일 | `/api/token` | `HttpOnly; Secure; SameSite=Lax` |

- `/login`, `/signup`, `/api/token` 을 제외한 모든 요청은 Access Token이 필요합니다. 브라우저는 `access_token` 쿠키를 자동으로 보내고, 쿠키를 쓸 수 없는 클라이언트는 `Authorization: Bearer <access token>` 헤더를 사용할 수 있습니다(헤더 우선).
- `refresh_token` 쿠키는 Path가 `/api/token` 이라 재발급 요청에만 전송됩니다.
- 세션은 사용하지 않습니다(stateless). CSRF 토큰 대신 `SameSite=Lax` 로 cross-site 요청에 쿠키가 실리지 않게 막습니다.
- `Secure` 쿠키이므로 브라우저는 HTTPS(또는 `localhost`)에서만 쿠키를 저장·전송합니다.
- 인증 실패는 `401`, 잘못된 입력은 `400` 을 [Problem Details](https://www.rfc-editor.org/rfc/rfc9457) 형식으로 반환합니다.

## API

### `POST /signup` — 회원가입

```json
// Request
{ "email": "user@example.com", "password": "password" }
```

- `201 Created`
- `400 Bad Request`: 이미 가입된 이메일

### `POST /login` — 로그인

```json
// Request
{ "email": "user@example.com", "password": "password" }
```

- `204 No Content` + `Set-Cookie: access_token=…`, `Set-Cookie: refresh_token=…`
- `401 Unauthorized`: 이메일 또는 비밀번호 불일치
- 사용자당 Refresh Token은 하나만 유지되며, 다시 로그인하면 이전 Refresh Token은 무효가 됩니다.

### `POST /api/token` — Access Token 재발급

요청 본문 없이 `refresh_token` 쿠키로 요청합니다.

- `204 No Content` + `Set-Cookie: access_token=…`
- `401 Unauthorized`: 쿠키가 없거나, 만료·위조되었거나, 저장된 Refresh Token과 다른 경우

### curl 예시

```bash
curl -i -H 'Content-Type: application/json' -d '{"email":"a@b.com","password":"pw1234"}' localhost:8080/signup
curl -i -c cookies.txt -H 'Content-Type: application/json' -d '{"email":"a@b.com","password":"pw1234"}' localhost:8080/login
curl -i -b cookies.txt -c cookies.txt -X POST localhost:8080/api/token
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
├── config        # WebSecurityConfig, TokenAuthenticationFilter, GlobalExceptionHandler
│   └── jwt       # JwtProperties, TokenProvider, TokenCookies
├── controller    # UserApiController, TokenApiController
├── domain        # User, RefreshToken
├── dto
├── repository
└── service       # TokenService, RefreshTokenService, UserService, UserDetailService
```

## TODO

- 로그아웃 API: HttpOnly 쿠키는 클라이언트에서 지울 수 없으므로 서버가 쿠키 만료와 Refresh Token 삭제를 해줘야 합니다.
- 요청 값 검증(`spring-boot-starter-validation`): 이메일 형식, 비밀번호 길이 등
- Access / Refresh Token 구분: 지금은 두 토큰의 형식이 같아서 Refresh Token을 `Authorization` 헤더에 넣으면 Access Token처럼 동작합니다.

## 라이선스

[MIT License](LICENSE) © 2023 pink-spider
