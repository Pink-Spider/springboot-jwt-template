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

- **Token creation/validation:** `TokenProvider` uses jjwt 0.13 and signs HS256 with `JwtProperties.signingKey()`. The key is the UTF-8 bytes of `jwt.secret_key` and must be at least 32 bytes. `JwtFactory` (test sources) builds arbitrary tokens for tests and must stay in sync with how `TokenProvider` signs.
- **Refresh flow:** `POST /api/token` → `TokenService.createNewAccessToken` validates the refresh token signature, looks it up in `RefreshToken` (one row per user, `user_id` unique), loads the `User`, and issues a 2-hour access token. The token's `sub` is the user's email and the `id` claim is the user id.
- **Users:** `User` is the JPA entity (table `users`) and implements `UserDetails` directly. `UserDetailService` loads by email. `UserService` stores BCrypt-encoded passwords.
- **Security:** `WebSecurityConfig` permits `/login`, `/signup`, `/user` and requires auth elsewhere. It ignores the H2 console and `/static/**`, and exposes a `ProviderManager`/`DaoAuthenticationProvider` `AuthenticationManager`.

Known gaps inherited from the Boot 3 implementation (behavior was kept the same during migration):
- `TokenAuthenticationFilter` (reads `Authorization: Bearer …`) exists but is **not registered** in the `SecurityFilterChain`, so bearer tokens do not authenticate requests yet.
- `/api/token` is not in the permit list and CSRF is enabled. `TokenApiControllerTest` passes only because it rebuilds `MockMvc` without `springSecurity()`, which bypasses the security filters.
- `/signup` and `/user` are permitted, but no controller exists for them yet.

## Boot 4 specifics

- Boot 4 uses modular starters: `spring-boot-starter-webmvc` (not `-web`), `spring-boot-h2console` (required for the H2 console and `PathRequest.toH2Console()`), and the per-module test starters (`-webmvc-test`, `-security-test`, `-data-jpa-test`).
- Relocated packages: `org.springframework.boot.security.autoconfigure.web.servlet.PathRequest` and `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- Jackson 3 is the default: inject `tools.jackson.databind.ObjectMapper`, not `com.fasterxml…`. Jackson 2 is also on the classpath only because `jjwt-jackson` needs it internally.
- Spring Security 7 accepts only the lambda DSL. `.and()` and `authorizeRequests()` are gone.
