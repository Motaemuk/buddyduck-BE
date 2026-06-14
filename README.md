# buddyduck-BE

Concert Buddy Spring Boot backend.

## Requirements

- Java 17
- Gradle wrapper included

If the machine default Java is newer than the Gradle wrapper supports, run commands with Java 17:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test
```

## Local Run

The default `local` profile uses an in-memory H2 database so the app can boot without secrets.

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
