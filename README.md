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

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Stop local MySQL:

```bash
docker compose down
```

Remove the local MySQL volume when you want a clean database:

```bash
docker compose down -v
```

## Kakao OAuth

The frontend receives a Kakao authorization code, then sends it to this backend. The backend exchanges the code for a Kakao token, reads Kakao user info, creates or finds the user, and returns a service JWT.

Local environment values:

```properties
JWT_SECRET_KEY=replace-with-at-least-32-byte-secret-key
JWT_ACCESS_EXPIRATION=3600000
KAKAO_CLIENT_ID=replace-with-kakao-rest-api-key
KAKAO_CLIENT_SECRET=
KAKAO_ALLOWED_REDIRECT_URIS=http://localhost:5173/oauth/kakao/callback
# Optional. If omitted, Kakao Local uses KAKAO_CLIENT_ID.
KAKAO_LOCAL_REST_API_KEY=replace-with-kakao-rest-api-key
KOPIS_SERVICE_KEY=
KOPIS_MAX_SYNC_ROWS=10
KOPIS_SYNC_ON_QUERY=false
```

Kakao Developers setup:

- Create a Kakao app and enable Kakao Login.
- Register the local frontend domain, for example `http://localhost:5173`.
- Register the exact redirect URI used by the frontend, for example `http://localhost:5173/oauth/kakao/callback`.
- Copy the REST API key to `KAKAO_CLIENT_ID`.
- Optionally copy the same REST API key to `KAKAO_LOCAL_REST_API_KEY` for Kakao Local place search. If this value is omitted, the backend falls back to `KAKAO_CLIENT_ID`.
- If Kakao client secret is enabled, copy it to `KAKAO_CLIENT_SECRET`; otherwise leave it empty.
- Configure the profile nickname consent item. Age range and gender are stored when Kakao returns them; otherwise they are saved as `PRIVATE`.

Login request:

```bash
curl -X POST http://localhost:8080/api/auth/kakao/login \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "<kakao-authorization-code>",
    "redirectUri": "http://localhost:5173/oauth/kakao/callback"
  }'
```

Login response result:

```json
{
  "accessToken": "<service-jwt>",
  "isNewUser": true,
  "profileCompleted": false,
  "user": {
    "id": 1,
    "nickname": "duck_fan"
  }
}
```

Use the service JWT for protected APIs:

```bash
curl http://localhost:8080/api/users/me \
  -H 'Authorization: Bearer <service-jwt>'
```

## Kakao Local

`GET /api/places/search` and `GET /api/places/geocode` call Kakao Local when `KAKAO_LOCAL_REST_API_KEY` or `KAKAO_CLIENT_ID` is configured. If no key is configured, they fall back to locally stored `places` rows, which is useful for tests and offline local development.

Kakao Developers setup:

- Open your Kakao app in Kakao Developers.
- Go to App settings and copy the REST API key.
- Put the key in `.env` as `KAKAO_LOCAL_REST_API_KEY`, or rely on the existing `KAKAO_CLIENT_ID` fallback when the same Kakao app key is used.
- No redirect URI is needed for Kakao Local. Redirect URI is only for Kakao Login.

Required request header used by the backend:

```text
Authorization: KakaoAK <REST_API_KEY>
```

## KOPIS Concert Sync

`GET /api/concerts` is DB-backed by default. For the MVP demo, run a one-time KOPIS import first, then let the home/search screen query the cached `concerts` rows.

Local or server environment values:

```properties
KOPIS_SERVICE_KEY=<kopis-service-key>
KOPIS_MAX_SYNC_ROWS=10
KOPIS_SYNC_ON_QUERY=false
KOPIS_INITIAL_IMPORT_DAYS=30
KOPIS_INITIAL_IMPORT_ROWS=100
KOPIS_INITIAL_IMPORT_MAX_PAGES=100
```

One-time import command:

```bash
KOPIS_INITIAL_IMPORT_ENABLED=true \
SPRING_MAIN_WEB_APPLICATION_TYPE=none \
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
./gradlew bootRun
```

On EC2 Docker Compose, run the same app image as a one-off container with `KOPIS_INITIAL_IMPORT_ENABLED=true` and `SPRING_MAIN_WEB_APPLICATION_TYPE=none`. Do not keep `KOPIS_INITIAL_IMPORT_ENABLED=true` in the normal server `.env.prod`.

Notes:

- Do not commit the real KOPIS key.
- KOPIS date range requests are capped to 31 days. With the default `KOPIS_INITIAL_IMPORT_DAYS=30`, the import covers today through today plus 30 days.
- KOPIS concert detail can include `poster`, `area`, `genrenm`, and `dtguidance`. Synced rows expose them as `posterUrl`, `area`, `genre`, and `timeGuidance`.
- `dtguidance` is a free-form text guide. If it contains exactly one distinct `HH:mm`, the sync sets `startAt` to that time on the start date. If it has multiple times or no time, `startAt` falls back to `00:00`. `endAt` is stored as the end date at `23:59:59`.
- `openRoomCount` is calculated from current `OPEN` rooms, not from KOPIS.

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

## EC2 Docker Deployment

EC2 Docker/Caddy deployment notes are in [docs/deploy-ec2.md](docs/deploy-ec2.md).

## Current Scope

- Spring Boot foundation
- Common API response envelope
- Common exception handler
- `GET /api/health`
- `POST /api/auth/kakao/login`
- JWT bearer authentication
- `GET /api/users/me`
- `PATCH /api/users/me/profile`
- Swagger UI and OpenAPI JSON
- ERD-based JPA domain skeleton and repositories
- Local MySQL Docker Compose
- Flyway schema migration baseline
