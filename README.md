# buddyduck-BE

Concert Buddy Spring Boot backend.

## Requirements

- Java 17
- Gradle wrapper included
- Docker or Colima for local MySQL

If the machine default Java is newer than the Gradle wrapper supports, run commands with Java 17:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test
```

## Local Run

The default `local` profile uses MySQL. Copy `.env.example` to `.env`, then edit `.env` if you want different local credentials.

```bash
cp .env.example .env
```

Start local MySQL:

```bash
docker compose up -d mysql
```

Run the app:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew bootRun
```

Health check:

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공했습니다.",
  "result": {
    "status": "UP"
  }
}
```

Stop local MySQL:

```bash
docker compose down
```

Remove the local MySQL volume when you want a clean database:

```bash
docker compose down -v
```

## Test Profile

Tests use the `test` profile with in-memory H2. They do not require Docker.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test
```

## Migration

Flyway runs versioned SQL migrations from:

```text
src/main/resources/db/migration
```

Current baseline:

```text
V1__init_schema.sql
```

## RDS Profile

Use the `prod` profile for MySQL/RDS. Set values in the server environment, not in git-tracked files.

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<rds-endpoint>:3306/<database-name>
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
```

## Current Scope

- Spring Boot foundation
- Common API response envelope
- Common exception handler
- `GET /api/health`
- ERD-based JPA domain skeleton and repositories
- Local MySQL Docker Compose
- Flyway schema migration baseline
