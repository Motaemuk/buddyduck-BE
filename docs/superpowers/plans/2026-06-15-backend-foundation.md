# Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Spring Boot backend foundation with `/api/health`, environment-based DB configuration, and ERD-aligned domain skeletons.

**Architecture:** Use a small UMC-style package layout: `global` for shared response/error/config/entity infrastructure and `domain/<name>` for Concert Buddy entities and repositories. Keep real API behavior limited to health for this branch; domain files are schema skeletons only.

**Tech Stack:** Java 17, Spring Boot 3.5.x, Gradle, Spring Web, Validation, Spring Data JPA, MySQL Connector/J, Lombok, JUnit, MockMvc.

---

### Task 1: Project Scaffold

**Files:**
- Create: Gradle/Spring Boot project files
- Create: `src/main/java/com/buddyduck/buddyduck/BuddyduckApplication.java`
- Create: `src/main/resources/application.yml`

- [ ] Generate or create a minimal Spring Boot Gradle project.
- [ ] Configure Java 17, Web, Validation, Data JPA, MySQL, Lombok, and tests.
- [ ] Configure datasource values from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- [ ] Run `./gradlew test`.
- [ ] Commit: `feat: backend foundation 구성`

### Task 2: Common API Response And Health

**Files:**
- Create: `src/test/java/com/buddyduck/buddyduck/domain/health/controller/HealthControllerTest.java`
- Create: `src/main/java/com/buddyduck/buddyduck/global/apiPayload/*`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/health/controller/HealthController.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/health/dto/HealthResponseDto.java`

- [ ] Write a failing MockMvc test for `GET /api/health`.
- [ ] Implement common response envelope and health controller.
- [ ] Run targeted health test, then full `./gradlew test`.
- [ ] Commit: `feat: health check API 구현`

### Task 3: ERD Domain Skeleton

**Files:**
- Create: `src/main/java/com/buddyduck/buddyduck/global/entity/BaseTimeEntity.java`
- Create: `src/main/java/com/buddyduck/buddyduck/global/config/JpaConfig.java`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/*/entity/*`
- Create: `src/main/java/com/buddyduck/buddyduck/domain/*/repository/*`

- [ ] Add JPA auditing base entity.
- [ ] Add ERD enum/entity/repository skeletons for users, concerts, places, rooms, room members, join requests, schedules, slots, and route segments.
- [ ] Run `./gradlew test`.
- [ ] Commit: `feat: ERD 기반 domain skeleton 추가`

### Task 4: Developer Docs And Continuity

**Files:**
- Modify: `README.md`
- Modify: `/Users/chataehun/inha-learn/findings.md`
- Modify: `/Users/chataehun/inha-learn/progress.md`
- Modify: `/Users/chataehun/inha-learn/CONTINUITY.md`

- [ ] Document local run and environment variable names without secrets.
- [ ] Keep root context files short and update only durable outcomes.
- [ ] Run final `./gradlew test` and `git status --short`.
- [ ] Commit docs if README changes are useful: `docs: backend 실행 방법 정리`
