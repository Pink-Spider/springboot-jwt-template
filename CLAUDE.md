# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Branches

- `main`: Spring Boot 4.1.1 / Java 25 / Gradle 9.8.2.
- `spring-boot-3x`: the original Spring Boot 3.0.2 / Java 17 / Gradle 7.6.1 implementation (jjwt 0.9.1). Gradle 7.6 cannot run on JDK 20+, so build that branch with `JAVA_HOME` pointing at JDK 17–19.

The package `io.pinkspider.springboot3jwttemplate` and project name `springboot3_jwt_template` still carry the "3" on `main`; they were not renamed during the Boot 4 migration.

## Commands

```bash
./gradlew build                                              # compile + test + package
./gradlew bootRun --args='--spring.profiles.active=local'   # run with H2 + jwt settings (port 8080)
./gradlew test                                               # all tests (JUnit 5)
./gradlew test --tests '*TokenProviderTest'                  # single class
./gradlew test --tests '*TokenProviderTest.validToken'       # single method
```

The Java 25 toolchain is resolved by Gradle (foojay resolver in `settings.gradle`), so the JDK running Gradle only needs to be 17+. No linter or formatter is configured.

## Configuration

Only `application-local.yml` has content (`application.yml`, `-dev`, `-test` are empty). It holds the in-memory H2 datasource, H2 console, and the `jwt.issuer` / `jwt.secret_key` values bound to `JwtProperties`. Tests that need these use `@ActiveProfiles("local")`, and `bootRun` needs the `local` profile too.

## Architecture

Everything lives under `global/` (config, controller, domain, dto, repository, service).

- **Token creation/validation:** `TokenProvider` uses jjwt 0.13 and signs HS256 with `JwtProperties.signingKey()`. The key is the UTF-8 bytes of `jwt.secret_key` and must be at least 32 bytes. Every token carries a `token_type` claim (`TokenType.ACCESS`/`REFRESH`), and `validToken(token, type)` requires it to match, so the filter accepts only access tokens and `/api/token` accepts only refresh tokens. Both share one signing key, so this claim is the only thing separating them. Never add a `validToken` overload that skips the type check. `JwtFactory` (test sources) builds arbitrary tokens for tests. It defaults to `ACCESS` (`.tokenType(TokenType.REFRESH)` for refresh tokens) and must stay in sync with how `TokenProvider` signs.
- **Cookies:** tokens never appear in response bodies. `TokenCookies` builds every `Set-Cookie` (`HttpOnly; Secure; SameSite=Lax`). `access_token` uses Path `/` and lasts 2h. `refresh_token` uses Path `/api/token` and lasts 14d, so browsers send it only to the refresh endpoint. The durations live as constants on `TokenService`. `SameSite=Lax` stands in for CSRF protection, which is disabled. Keep that in mind before adding state-changing GET endpoints or relaxing SameSite.
- **Login:** `POST /login` (`UserApiController`) authenticates through the exposed `AuthenticationManager` (`ProviderManager` + `DaoAuthenticationProvider` + `UserDetailService`) and gets the `User` entity back as the principal. `TokenService.createAccessToken`/`createRefreshToken` issue the tokens, and `RefreshTokenService.saveOrUpdate` stores the refresh token. There is one row per user (`user_id` unique), so a new login invalidates the previous refresh token.
- **Signup:** `POST /signup` → `UserService.save` rejects a duplicate email with `IllegalArgumentException` (→ 400) and stores a BCrypt-encoded password.
- **Refresh:** `POST /api/token` reads the `refresh_token` cookie. `TokenService.createNewAccessToken` checks the signature and that the token matches the stored row, then sets a new `access_token` cookie. The refresh token is not rotated.
- **Logout:** `DELETE /api/token`, deliberately on the refresh cookie's path so it receives `refresh_token` even after the access token has expired. `TokenService.deleteRefreshToken` deletes the matching row without validating the JWT, and the response expires both cookies via `TokenCookies.expire*()` (same name + Path, `Max-Age=0`). It is permitted, idempotent, and always returns 204. Issued access tokens stay valid until they expire because there is no blocklist.
- **Users:** `User` is the JPA entity (table `users`) and implements `UserDetails` directly. `UserDetailService` loads by email and throws `UsernameNotFoundException`, which Spring turns into `BadCredentialsException` so a response never reveals whether an email exists.
- **Security:** `WebSecurityConfig` is a stateless API chain. CSRF, sessions, form login, HTTP Basic, and logout are disabled. `TokenAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`. It takes the token from `Authorization: Bearer …` first, then from the `access_token` cookie, and sets the `SecurityContext` (principal = email, `ROLE_USER`). `/login`, `/signup`, `/api/token` and all `ERROR` dispatches are permitted. Everything else requires auth, and unauthenticated requests get `401` (`HttpStatusEntryPoint`). The H2 console and `/static/**` bypass security entirely.
  - The `ERROR` dispatch permit matters: with STATELESS, and with the `OncePerRequestFilter` skipping error dispatches, a forward to `/error` would otherwise be anonymous and turn every 404/500 for an authenticated user into a 401. MockMvc does not perform error forwards, so tests cannot catch a regression here. Verify against a running app.
- **Errors:** `GlobalExceptionHandler` maps any `AuthenticationException` to 401 and `IllegalArgumentException` to 400, both as `ProblemDetail`. Token/login failures in services throw `BadCredentialsException` so they land on 401.
- **Tests** use `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("local")`, so `MockMvc` runs through the real security filter chain. `TokenAuthenticationFilterTest` hits an unmapped protected path: `404` means authentication passed, `401` means it failed. Test classes share one cached context and one in-memory H2 database, so clean `RefreshToken` and `User` rows in `@BeforeEach` and avoid reusing emails across classes.

## Boot 4 specifics

- Boot 4 uses modular starters: `spring-boot-starter-webmvc` (not `-web`), `spring-boot-h2console` (required for the H2 console and `PathRequest.toH2Console()`), and the per-module test starters (`-webmvc-test`, `-security-test`, `-data-jpa-test`).
- Relocated packages: `org.springframework.boot.security.autoconfigure.web.servlet.PathRequest` and `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- Jackson 3 is the default: inject `tools.jackson.databind.ObjectMapper`, not `com.fasterxml…`. Jackson 2 is also on the classpath only because `jjwt-jackson` needs it internally.
- Spring Security 7 accepts only the lambda DSL. `.and()` and `authorizeRequests()` are gone.
