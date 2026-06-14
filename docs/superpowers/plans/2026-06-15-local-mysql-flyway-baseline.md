# Local MySQL Flyway Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make local development use MySQL through Docker Compose and manage schema creation with Flyway migrations.

**Architecture:** Keep `local` close to production by using MySQL locally, keep tests fast and isolated with H2 through the `test` profile, and reuse the same Flyway migration SQL for local MySQL and later RDS. Commit `.env.example` only; keep real `.env` ignored.

**Tech Stack:** Spring Boot 3.5.x, Gradle, Flyway, MySQL 8.4 Docker image, H2 test profile, JUnit.

---

### Task 1: Local MySQL Compose And Env

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create untracked local file: `.env`

- [ ] Add MySQL service with database/user/password variables from `.env`.
- [ ] Add `.env.example` with placeholder local development values.
- [ ] Add ignored `.env` for this machine only.
- [ ] Run `docker compose config`.
- [ ] Commit: `feat: local MySQL compose 구성`

### Task 2: Flyway Dependency And Profiles

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml`
- Create: `src/test/resources/application.yml`

- [ ] Add Flyway dependencies for core and MySQL support.
- [ ] Change `local` profile datasource to MySQL env values.
- [ ] Add `test` profile using H2 so tests do not require Docker.
- [ ] Run `./gradlew test`.

### Task 3: Initial Schema Migration

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/DomainRepositoryContextTest.java`

- [ ] Add ERD-based initial MySQL schema.
- [ ] Verify test profile starts with Flyway and repositories.
- [ ] Run local MySQL and boot app once against MySQL.
- [ ] Commit: `feat: Flyway migration baseline 추가`

### Task 4: Docs And Handoff

**Files:**
- Modify: `README.md`
- Modify: `/Users/chataehun/inha-learn/CONTINUITY.md`
- Modify: `/Users/chataehun/inha-learn/findings.md`
- Modify: `/Users/chataehun/inha-learn/progress.md`

- [ ] Document local DB start, app boot, and health check.
- [ ] Update root continuity files briefly.
- [ ] Run final `./gradlew test` and `git status --short`.
- [ ] Commit: `docs: local DB 실행 방법 정리`
