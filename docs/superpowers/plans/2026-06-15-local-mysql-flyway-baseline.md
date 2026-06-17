# Local MySQL Flyway Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make local development use MySQL through Docker Compose and manage schema creation with Flyway migrations.

**Architecture:** Keep `local` close to production by using MySQL locally, keep tests fast and isolated with H2 through the `test` profile, and reuse the same Flyway migration SQL for local MySQL and later RDS. Commit `.env.example` only; keep real `.env` ignored.

**Tech Stack:** Spring Boot 3.5.x, Gradle, Flyway, MySQL 8.4 Docker image, H2 test profile, JUnit.

---

## Task 1: Local MySQL Compose And Env

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create untracked local file: `.env`

- [x] Add MySQL service with database/user/password variables from `.env`.
- [x] Add `.env.example` with placeholder local development values.
- [x] Add ignored `.env` for this machine only.
- [x] Run `docker compose config`.
- [x] Commit: `feat: local MySQL compose 구성`

## Task 2: Flyway Dependency And Profiles

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml`
- Create: `src/test/resources/application.yml`

- [x] Add Flyway dependencies for core and MySQL support.
- [x] Change `local` profile datasource to MySQL env values.
- [x] Add `test` profile using H2 so tests do not require Docker.
- [x] Run `./gradlew test`.

## Task 3: Initial Schema Migration

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`
- Test: `src/test/java/com/buddyduck/buddyduck/domain/DomainRepositoryContextTest.java`

- [x] Add ERD-based initial MySQL schema.
- [x] Verify test profile starts with Flyway and repositories.
- [x] Run local MySQL and boot app once against MySQL.
- [x] Commit: `feat: Flyway migration baseline 추가`

## Task 4: Docs And Handoff

**Files:**
- Modify: `README.md`
- Update external continuity notes outside this repository when needed.

- [x] Document local DB start, app boot, and health check.
- [x] Update root continuity files briefly.
- [x] Run final `./gradlew test` and `git status --short`.
- [x] Commit: `docs: local DB 실행 방법 정리`
