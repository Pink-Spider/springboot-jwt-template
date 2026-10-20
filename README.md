# Spring Boot 4 JWT Template

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.8.2-02303A?logo=gradle&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

Spring Boot 4 / Spring Security 7 기반의 JWT 인증 템플릿입니다. 회원가입·로그인·로그아웃과 Access Token / Refresh Token 발급·재발급을 **HttpOnly 쿠키** 기반으로 구현했습니다.

> Spring Boot 3 버전은 [`spring-boot-3x`](https://github.com/Pink-Spider/springboot-jwt-template/tree/spring-boot-3x) 브랜치에 있습니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring Framework 7.0) |
| Web | Spring Web MVC, Tomcat 11 |
| Security | Spring Security 7.1, jjwt 0.13.0 (HS256) |
| Data | Spring Data JPA 4.1, Hibernate 7.4, H2 (in-memory) |
| JSON | Jackson 3 |
| Validation | Jakarta Bean Validation |
| Build | Gradle 9.8.2 (Java toolchain + foojay resolver) |
| 기타 | Lombok |

## 브랜치

| 브랜치 | Spring Boot | Java | Gradle | jjwt | 내용 |
| --- | --- | --- | --- | --- | --- |
| `main` | 4.1.1 | 25 | 9.8.2 | 0.13.0 | 회원가입·로그인·로그아웃, HttpOnly 쿠키, 요청 값 검증 |
| `spring-boot-3x` | 3.0.2 | 17 | 7.6.1 | 0.9.1 | 이전 구현 (Access Token 재발급 API만, 토큰을 응답 본문으로 반환) |

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
- 두 토큰은 `token_type` claim(`access` / `refresh`)으로 구분됩니다. Refresh Token으로는 API를 인증할 수 없고, Access Token으로는 재발급할 수 없습니다.
- 세션은 사용하지 않습니다(stateless). CSRF 토큰 대신 `SameSite=Lax` 로 cross-site 요청에 쿠키가 실리지 않게 막습니다.
- `Secure` 쿠키이므로 브라우저는 HTTPS(또는 `localhost`)에서만 쿠키를 저장·전송합니다.
- 인증 실패는 `401`, 잘못된 입력(검증 실패, JSON 형식 오류)은 `400` 을 [Problem Details](https://www.rfc-editor.org/rfc/rfc9457) 형식으로 반환합니다.

## API

### `POST /signup` — 회원가입

```json
// Request
{ "email": "user@example.com", "password": "password1234" }
```

| 필드 | 규칙 |
| --- | --- |
| `email` | 필수, 이메일 형식 |
| `password` | 필수, 8~72자 (BCrypt 제한으로 UTF-8 72바이트 이하) |

- `201 Created`
- `400 Bad Request`: 검증 실패, 이미 가입된 이메일

검증 실패 시 필드별 메시지를 `errors` 로 반환합니다.

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Invalid request",
  "errors": {
    "email": "must be a well-formed email address",
    "password": "size must be between 8 and 72"
  }
}
```

### `POST /login` — 로그인

```json
// Request
{ "email": "user@example.com", "password": "password1234" }
```

- `204 No Content` + `Set-Cookie: access_token=…`, `Set-Cookie: refresh_token=…`
- `400 Bad Request`: `email` 또는 `password` 가 비어 있음 (형식·길이 정책은 회원가입에서만 검사)
- `401 Unauthorized`: 이메일 또는 비밀번호 불일치
- 사용자당 Refresh Token은 하나만 유지되며, 다시 로그인하면 이전 Refresh Token은 무효가 됩니다.

### `POST /api/token` — Access Token 재발급

요청 본문 없이 `refresh_token` 쿠키로 요청합니다.

- `204 No Content` + `Set-Cookie: access_token=…`
- `401 Unauthorized`: 쿠키가 없거나, 만료·위조되었거나, 저장된 Refresh Token과 다른 경우

### `DELETE /api/token` — 로그아웃

요청 본문 없이 `refresh_token` 쿠키로 요청합니다. Access Token이 만료된 상태에서도 호출할 수 있습니다.

- `204 No Content` + `Set-Cookie: access_token=; Max-Age=0`, `Set-Cookie: refresh_token=; Max-Age=0`
- 저장된 Refresh Token을 삭제하므로 이후 재발급이 불가능합니다.
- 쿠키가 없어도 `204` 를 반환합니다(여러 번 호출해도 안전).
- 이미 발급된 Access Token은 stateless JWT 특성상 만료(최대 2시간)까지 유효합니다. 브라우저의 쿠키는 삭제됩니다.

### curl 예시

```bash
curl -i -H 'Content-Type: application/json' -d '{"email":"a@b.com","password":"password1234"}' localhost:8080/signup
curl -i -c cookies.txt -H 'Content-Type: application/json' -d '{"email":"a@b.com","password":"password1234"}' localhost:8080/login
curl -i -b cookies.txt -c cookies.txt -X POST localhost:8080/api/token
curl -i -b cookies.txt -c cookies.txt -X DELETE localhost:8080/api/token   # 로그아웃
```

## 테스트

```bash
./gradlew test                                          # 전체
./gradlew test --tests '*TokenProviderTest'             # 특정 클래스
./gradlew test --tests '*TokenProviderTest.validToken'  # 특정 메서드
```

## 프로젝트 구조

패키지명(`springboot3jwttemplate`)은 Boot 3 시절의 이름을 그대로 유지하고 있습니다.

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

## Spring Boot 3 → 4 주요 변경 사항

`spring-boot-3x` 브랜치에서 마이그레이션하며 적용한 내용입니다.

| 영역 | Boot 3 | Boot 4 |
| --- | --- | --- |
| 스타터 | `spring-boot-starter-web` | `spring-boot-starter-webmvc` (기능별 모듈 분리) |
| H2 콘솔 | 자동 포함 | `spring-boot-h2console` 모듈 추가 필요 |
| 테스트 스타터 | `spring-boot-starter-test`, `spring-security-test` | `spring-boot-starter-webmvc-test`, `-security-test`, `-data-jpa-test` |
| 패키지 이동 | `o.s.boot.autoconfigure.security.servlet.PathRequest`<br>`o.s.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` | `o.s.boot.security.autoconfigure.web.servlet.PathRequest`<br>`o.s.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` |
| Jackson | 2 (`com.fasterxml.jackson`) | 3 (`tools.jackson`) |
| Security DSL | `authorizeRequests()`, `.and()` | lambda DSL만 지원 (`authorizeHttpRequests(...)`) |
| AuthenticationManager | `AuthenticationManagerBuilder` + `.and()` | `ProviderManager` + `DaoAuthenticationProvider(userDetailsService)` |
| jjwt | 0.9.1 (`String` 키, `jaxb-api` 필요) | 0.13.0 (`SecretKey`, `verifyWith` / `parseSignedClaims`) |
| Gradle | 7.6.1 | 9.x (`junit-platform-launcher` 명시 필요) |

## 라이선스

[MIT License](LICENSE) © 2023 pink-spider
