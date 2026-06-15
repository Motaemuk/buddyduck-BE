# Kakao OAuth JWT Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `POST /api/auth/kakao`, JWT bearer authentication, and `GET /api/users/me` for Concert Buddy.

**Architecture:** The frontend obtains a Kakao authorization code and sends it with `redirectUri` to the backend. The backend exchanges the code for a Kakao access token, retrieves user info, requires Kakao age/gender, upserts `users`, returns a service JWT, and uses a JWT filter for protected APIs.

**Tech Stack:** Spring Boot 3.5.15, Spring Security 6.5, JJWT 0.12.3, JPA, Flyway, H2 tests, Lombok.

---

### Task 1: Kakao User Info Mapping

**Files:**
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/kakao/dto/KakaoUserInfo.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/kakao/KakaoUserInfoMapper.java`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/auth/kakao/KakaoUserInfoMapperTest.java`

- [ ] Write failing tests for nickname, `age_range`, `gender`, missing required profile info, and age/gender enum mapping.
- [ ] Run `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test --tests '*KakaoUserInfoMapperTest'` and confirm failure.
- [ ] Implement the mapper and DTO.
- [ ] Run the mapper test and confirm pass.
- [ ] Commit `feat: Kakao user info 매핑 구현`.

### Task 2: Security Dependencies And JWT

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/java/com/buddyduck/buddyduck/global/security/AuthUser.java`
- Create: `src/main/java/com/buddyduck/buddyduck/global/security/JwtTokenProvider.java`
- Create: `src/test/java/com/buddyduck/buddyduck/global/security/JwtTokenProviderTest.java`

- [ ] Write failing JWT tests for access token creation, user id extraction, invalid token handling, and expired token handling.
- [ ] Run `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test --tests '*JwtTokenProviderTest'` and confirm failure.
- [ ] Add Spring Security/JWT dependencies and JWT config properties.
- [ ] Implement `AuthUser` and `JwtTokenProvider`.
- [ ] Run JWT tests and confirm pass.
- [ ] Commit `feat: JWT access token 발급 구현`.

### Task 3: Kakao Auth API

**Files:**
- Modify: `src/main/java/com/buddyduck/buddyduck/domain/user/entity/User.java`
- Modify: `src/main/java/com/buddyduck/buddyduck/domain/user/repository/UserRepository.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/controller/AuthController.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/dto/KakaoLoginRequest.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/dto/LoginResponse.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/dto/LoginUserSummary.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/exception/AuthErrorCode.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/service/AuthService.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/auth/kakao/KakaoAuthClient.java`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/auth/service/AuthServiceTest.java`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/auth/controller/AuthControllerTest.java`

- [ ] Write failing service tests for new user signup, existing user login, required Kakao age/gender failure, and nickname preservation on existing login.
- [ ] Run service tests and confirm failure.
- [ ] Implement user factory/update methods, repository lookups, auth DTOs, auth error code, Kakao client, and auth service.
- [ ] Run service tests and confirm pass.
- [ ] Write failing controller tests for `POST /api/auth/kakao` success and validation failure.
- [ ] Implement `AuthController`.
- [ ] Run controller tests and confirm pass.
- [ ] Commit `feat: Kakao OAuth login API 구현`.

### Task 4: JWT Security Filter And Profile API

**Files:**
- Create: `src/main/java/com/buddyduck/buddyduck/global/config/SecurityConfig.java`
- Create: `src/main/java/com/buddyduck/buddyduck/global/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/buddyduck/buddyduck/global/security/UserPrincipal.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/user/controller/UserController.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/user/dto/UserProfileResponse.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/user/service/UserQueryService.java`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/user/controller/UserControllerTest.java`
- Test: `src/test/java/com/buddyduck/buddyduck/global/security/SecurityConfigTest.java`

- [ ] Write failing tests for `/api/users/me` without token, invalid token, and valid token.
- [ ] Run tests and confirm failure.
- [ ] Implement security config, bearer token filter, principal, user query service, and user controller.
- [ ] Run tests and confirm pass.
- [ ] Commit `feat: JWT 인증 profile API 구현`.

### Task 5: Local Config Documentation

**Files:**
- Modify: `.env.example`
- Modify: `README.md`

- [ ] Add placeholder-only Kakao/JWT environment variables to `.env.example`.
- [ ] Document Kakao Developers setup values and local OAuth smoke-test flow in `README.md`.
- [ ] Run full test suite.
- [ ] Commit `docs: Kakao OAuth 설정 방법 정리`.
