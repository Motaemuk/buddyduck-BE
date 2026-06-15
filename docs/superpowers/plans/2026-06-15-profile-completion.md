# Profile Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Kakao OAuth create an authenticated but incomplete user, then require CB-02 profile completion before full service usage.

**Architecture:** `POST /api/auth/kakao` continues to authenticate and issue a JWT, but returns `profileCompleted`. `PATCH /api/users/me/profile` saves the CB-02 required profile fields and flips the completion flag. `profileSource` is not exposed as an API field; the source policy is documented instead.

**Tech Stack:** Spring Boot 3.5.15, Spring Security 6.5, JPA, Flyway, H2 tests, Lombok.

---

### Task 1: Backend Profile Completion API

**Files:**
- Modify: `src/main/java/com/buddyduck/buddyduck/domain/user/entity/User.java`
- Modify: `src/main/java/com/buddyduck/buddyduck/domain/auth/dto/LoginResponse.java`
- Modify: `src/main/java/com/buddyduck/buddyduck/domain/auth/service/AuthService.java`
- Modify: `src/main/java/com/buddyduck/buddyduck/domain/user/dto/UserProfileResponse.java`
- Modify: `src/main/java/com/buddyduck/buddyduck/domain/user/controller/UserController.java`
- Modify: `src/main/java/com/buddyduck/buddyduck/domain/user/service/UserQueryService.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/user/dto/UpdateProfileRequest.java`
- Create: `src/main/resources/db/migration/V2__add_profile_completion.sql`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/auth/service/AuthServiceTest.java`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/auth/controller/AuthControllerTest.java`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/user/controller/UserControllerTest.java`

- [ ] Write failing tests that login responses include `profileCompleted`, new Kakao users are incomplete, and `PATCH /api/users/me/profile` requires nickname, non-PRIVATE age range, non-PRIVATE gender, and saves visibility flags.
- [ ] Run targeted tests and confirm failure.
- [ ] Add Flyway V2 columns: `profile_completed`, `age_visible`, `gender_visible`.
- [ ] Implement entity methods, DTO fields, service logic, and controller endpoint.
- [ ] Run targeted tests and confirm pass.
- [ ] Run full test suite.
- [ ] Commit `feat: profile completion API 구현`.

### Task 2: API Spec And Product Docs

**Files:**
- Modify: `buddyduck-DOCS/docs/api/api-spec-concert-buddy.json`
- Modify: `buddyduck-DOCS/docs/api/api-erd-decisions.md`
- Modify: `buddyduck-DOCS/docs/api/external-api-integration.md`
- Modify: `buddyduck-DOCS/docs/product/screen-definition.md`

- [ ] Update `AUTH-002` response to include `profileCompleted`.
- [ ] Add or update `PATCH /api/users/me/profile` to require nickname, ageRange, gender, ageVisible, genderVisible.
- [ ] Document that `profileSource` is not an API field; Kakao-provided values may prefill CB-02 later, while MVP uses self-reported values.
- [ ] Commit `docs: profile completion API 명세 정리`.
