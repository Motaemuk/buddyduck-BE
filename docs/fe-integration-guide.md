# FE 연동 테스트 가이드

이 문서는 Concert Buddy FE가 배포 백엔드와 연동할 때 필요한 설정값, Kakao OAuth 흐름, 프로필 완료 처리, 기본 점검 순서를 정리한 가이드입니다.

## 1. 현재 배포 주소

| 구분 | 주소 |
| --- | --- |
| 백엔드 API | `https://api.boostad.site` |
| Swagger UI | `https://api.boostad.site/swagger-ui/index.html` |
| OpenAPI JSON | `https://api.boostad.site/v3/api-docs` |
| OpenAPI YAML | `https://api.boostad.site/v3/api-docs.yaml` |
| 프론트 운영 도메인 | `https://www.boostad.site`, `https://boostad.site` |

응답은 기본적으로 아래 wrapper 형태입니다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공했습니다.",
  "result": {}
}
```

## 2. FE env에 넣을 값

### 운영 배포용

```env
VITE_API_BASE_URL=https://api.boostad.site
VITE_KAKAO_REST_API_KEY=<카카오 앱 REST API 키>
VITE_KAKAO_REDIRECT_URI=https://www.boostad.site/oauth/kakao/callback
VITE_KAKAO_MAP_APP_KEY=<카카오 앱 JavaScript 키>
```

`boostad.site` apex 도메인으로도 서비스를 열 계획이면 `VITE_KAKAO_REDIRECT_URI`를 아래 값으로 맞춘 빌드도 가능합니다.

```env
VITE_KAKAO_REDIRECT_URI=https://boostad.site/oauth/kakao/callback
```

### 로컬 개발용

백엔드도 로컬에서 같이 띄우는 경우:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_KAKAO_REST_API_KEY=<카카오 앱 REST API 키>
VITE_KAKAO_REDIRECT_URI=http://localhost:5173/oauth/kakao/callback
VITE_KAKAO_MAP_APP_KEY=<카카오 앱 JavaScript 키>
```

FE만 로컬이고 배포 백엔드를 호출하는 경우:

```env
VITE_API_BASE_URL=https://api.boostad.site
VITE_KAKAO_REST_API_KEY=<카카오 앱 REST API 키>
VITE_KAKAO_REDIRECT_URI=http://localhost:5173/oauth/kakao/callback
VITE_KAKAO_MAP_APP_KEY=<카카오 앱 JavaScript 키>
```

운영 백엔드는 현재 `http://localhost:5173`, `https://www.boostad.site`, `https://boostad.site` origin을 CORS 허용합니다.

## 3. FE에 공유하지 않는 값

아래 값은 FE env나 클라이언트 코드에 넣지 않습니다.

| 값 | 이유 |
| --- | --- |
| `KAKAO_CLIENT_SECRET` | 카카오 OAuth 서버 측 secret입니다. 브라우저에 노출되면 안 됩니다. |
| `JWT_SECRET_KEY` | 백엔드 JWT 서명 secret입니다. |
| `DB_*` | RDS 접속 정보입니다. |
| `KOPIS_SERVICE_KEY` | 공연 데이터 초기 적재용 서버 키입니다. |
| `KAKAO_LOCAL_REST_API_KEY` | 백엔드가 장소 검색/주소 좌표 변환에 사용하는 Kakao Local REST 키입니다. FE 지도 렌더링에는 JavaScript 키를 사용합니다. |

헷갈리기 쉬운 구분은 아래와 같습니다.

| 목적 | 사용하는 키 | 사용하는 쪽 |
| --- | --- | --- |
| 카카오 로그인 인가 URL의 `client_id` | REST API 키 | FE |
| 카카오 토큰 교환 | REST API 키, 필요 시 Client Secret | BE |
| 지도 렌더링 | JavaScript 키 | FE |
| 장소명 검색, 주소 -> 좌표 변환 | REST API 키 | BE |

`VITE_KAKAO_REST_API_KEY`는 카카오 인가 URL의 `client_id`로 쓰입니다. 브라우저에 들어가는 값이지만 OAuth public client의 식별자에 가깝고 비밀값은 아닙니다. 숨겨야 하는 값은 `KAKAO_CLIENT_SECRET`입니다.

## 4. Kakao Developers 설정

카카오 개발자 콘솔에서 FE 기준 origin과 callback을 등록합니다.

### JavaScript SDK 도메인

아래 값을 JavaScript SDK 도메인에 등록합니다.

```text
http://localhost:5173
https://www.boostad.site
https://boostad.site
```

Vercel preview 도메인에서 직접 테스트해야 하면 해당 preview origin도 추가해야 합니다.

### 카카오 로그인 Redirect URI

아래 값을 카카오 로그인 Redirect URI에 등록합니다.

```text
http://localhost:5173/oauth/kakao/callback
https://www.boostad.site/oauth/kakao/callback
https://boostad.site/oauth/kakao/callback
```

FE가 실제로 사용하는 `VITE_KAKAO_REDIRECT_URI`, 카카오 인가 URL의 `redirect_uri`, 백엔드 `POST /api/auth/kakao/login` 요청의 `redirectUri`, 백엔드 서버 env의 `KAKAO_ALLOWED_REDIRECT_URIS`가 모두 정확히 맞아야 합니다.

## 5. 카카오 로그인 연동 흐름

### 1단계. FE에서 카카오 인가 URL로 이동

```ts
const params = new URLSearchParams({
  response_type: "code",
  client_id: import.meta.env.VITE_KAKAO_REST_API_KEY,
  redirect_uri: import.meta.env.VITE_KAKAO_REDIRECT_URI,
});

window.location.href = `https://kauth.kakao.com/oauth/authorize?${params.toString()}`;
```

### 2단계. FE callback에서 code 확인

카카오는 아래 형태로 FE callback 페이지로 돌려줍니다.

```text
https://www.boostad.site/oauth/kakao/callback?code=...
```

FE는 URL의 `code`를 읽습니다.

```ts
const params = new URLSearchParams(window.location.search);
const code = params.get("code");

if (!code) {
  throw new Error("Kakao authorization code is missing");
}
```

### 3단계. FE가 백엔드 로그인 API 호출

```http
POST /api/auth/kakao/login
Content-Type: application/json
```

```json
{
  "code": "<카카오가 넘겨준 인가 코드>",
  "redirectUri": "https://www.boostad.site/oauth/kakao/callback"
}
```

`redirectUri`는 1단계 인가 URL에 넣었던 값과 동일해야 합니다.

### 4단계. 로그인 응답 처리

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공했습니다.",
  "result": {
    "accessToken": "<jwt>",
    "isNewUser": true,
    "profileCompleted": false,
    "user": {
      "id": 1,
      "nickname": "kakao_user"
    }
  }
}
```

FE는 `result.accessToken`을 이후 API 호출의 Authorization header에 넣습니다.

```http
Authorization: Bearer <jwt>
```

`profileCompleted=false`이면 CB-02 프로필 입력 화면으로 이동시킵니다.

## 6. 프로필 완료 처리

우리 서비스는 MVP 기준으로 연령대와 성별이 필수입니다. 비공개 옵션은 사용하지 않습니다.

프로필 미완료 사용자는 아래 API만 사용할 수 있습니다.

- `GET /api/users/me`
- `PATCH /api/users/me/profile`
- 공개 공연 조회 API
- auth, health, Swagger/OpenAPI

프로필 완료 요청:

```http
PATCH /api/users/me/profile
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "nickname": "moon_armies",
  "ageRange": "TWENTIES",
  "gender": "FEMALE"
}
```

사용 가능한 enum:

| 필드 | 값 |
| --- | --- |
| `ageRange` | `TEENS`, `TWENTIES`, `THIRTIES`, `FORTIES_PLUS` |
| `gender` | `FEMALE`, `MALE` |

성공하면 응답의 `profileCompleted`가 `true`가 됩니다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공했습니다.",
  "result": {
    "id": 1,
    "nickname": "moon_armies",
    "ageRange": "TWENTIES",
    "gender": "FEMALE",
    "profileCompleted": true,
    "avatarColor": "#F5C542"
  }
}
```

## 7. 보호 API 호출 규칙

보호 API는 아래 헤더가 필요합니다.

```http
Authorization: Bearer <jwt>
```

프로필 미완료 사용자가 보호 API를 호출하면 아래 에러가 올 수 있습니다.

```json
{
  "isSuccess": false,
  "code": "AUTH_REQUIRED_PROFILE_INFO",
  "message": "추가 프로필 입력이 필요합니다.",
  "result": null
}
```

FE는 이 응답을 받으면 CB-02 프로필 입력 화면으로 보내면 됩니다.

## 8. 장소/지도 연동

FE 지도 렌더링은 Kakao JavaScript 키를 사용합니다.

```env
VITE_KAKAO_MAP_APP_KEY=<카카오 앱 JavaScript 키>
```

장소명 검색과 주소 -> 좌표 변환은 BE API를 사용합니다. FE에서 Kakao Local REST API를 직접 호출하지 않습니다.

```http
GET /api/places/search?keyword=잠실
Authorization: Bearer <jwt>
```

```http
GET /api/places/geocode?address=서울 송파구 올림픽로 424
Authorization: Bearer <jwt>
```

검색/지오코딩 결과는 후보 목록입니다. 사용자가 장소를 선택한 뒤에는 `POST /api/places`를 호출해서 `placeId`를 받아야 방 생성, 일정 저장 같은 API에 연결할 수 있습니다.
`/api/places/search` 응답의 `providerPlaceId`는 선택한 장소를 다시 저장할 때 그대로 `POST /api/places` 요청에 넣어 주세요. 이 값을 같이 보내면 같은 Kakao 장소를 중복 생성하지 않고 재사용할 수 있습니다.

## 9. 일정 경로 계산 연동

CB-11 일정 편집에서는 시작/도착 기준도 함께 보냅니다.

```json
{
  "customStartAt": "2026-06-15T14:00:00+09:00",
  "targetArrivalAt": "2026-06-15T18:30:00+09:00",
  "arrivalBufferMinutes": 30
}
```

- `customStartAt`: 사용자가 수정 화면에서 확정한 실제 시작 시간입니다. 사용자가 권장 시작 시간을 적용하면 이 값에 `recommendedStartAt`을 넣어 다시 요청하면 됩니다.
- `targetArrivalAt`: 공연장 도착 목표 시간입니다. 값이 없으면 BE는 공연 시작 시간에서 `arrivalBufferMinutes`를 뺀 시간을 사용합니다.
- `recommendedStartAt`: BE가 계산해서 내려주는 권장 시작 시간입니다. 읽기 전용 안내값으로 사용하면 됩니다.
- `fitStatus=OVERRUN`: 사용자 시작 시간 기준으로 목표 도착 시간을 넘긴 상태입니다. 이때 `overrunMinutes`를 CB-11' 경고에 표시하면 됩니다.
- `spareMinutes`: 목표 도착 시간까지 남는 여유 시간입니다.

`POST /api/schedules/{scheduleId}/draft/recalculate` 응답의 `slots`에는 계산된 `startAt`, `endAt`이 포함됩니다. FE는 장소 추가, 삭제, 순서 변경, 머무는 시간 변경, 이동수단 변경, 시작/도착 기준 변경 후 draft 요청을 보내고 이 값을 화면에 반영하면 됩니다.

CB-11 일정 편집에서는 route segment마다 이동 수단과 수동 조정 여부를 함께 보냅니다.

```json
{
  "fromClientId": "slot-meeting",
  "toClientId": "slot-cafe",
  "mode": "WALK",
  "durationMinutes": 18,
  "manuallyAdjusted": false
}
```

`manuallyAdjusted=false` 또는 생략이면 BE가 장소 좌표를 기준으로 거리/시간을 다시 계산합니다. 사용자가 이동 시간을 직접 `+/-`로 조정한 상태라면 `manuallyAdjusted=true`로 보내고, 이때는 FE가 보낸 `durationMinutes`를 그대로 사용합니다.

draft/timeline/map 응답의 route segment에는 아래 필드가 내려옵니다.

```json
{
  "mode": "CAR_TAXI",
  "distanceMeters": 1033,
  "durationMinutes": 5,
  "taxiFareWon": 3800,
  "tollFareWon": 0,
  "provider": "KAKAO_DRIVING",
  "manuallyAdjusted": false
}
```

`provider=DRIVING_DISTANCE_WALK_ESTIMATE`이면 도보 전용 API가 아니라 자동차 길찾기 거리 기반으로 도보 시간을 추정한 값입니다. `provider=FALLBACK_STRAIGHT_LINE`이면 Kakao Mobility 키가 꺼진 로컬/테스트 환경의 좌표 기반 추정값입니다. 더 자세한 계산 기준은 [일정 경로 계산 설계](schedule-route-planner.md)를 확인합니다.

## 10. 공연 검색 연동

공연 목록/상세 조회는 인증 없이 호출할 수 있습니다.

```http
GET /api/concerts
GET /api/concerts/{concertId}
```

공연 데이터는 FE 요청마다 KOPIS를 직접 호출하지 않고, 백엔드 DB cache를 조회합니다. 운영/발표 전에는 백엔드에서 KOPIS 초기 적재를 한 번 실행해 둔 데이터를 사용합니다.

## 11. 기본 점검 순서

FE 연동 시 아래 순서로 확인하면 원인을 좁히기 쉽습니다.

1. `GET https://api.boostad.site/api/health`가 200인지 확인합니다.
2. 카카오 authorize URL의 `client_id`가 REST API 키인지 확인합니다.
3. authorize URL의 `redirect_uri`가 Kakao Developers Redirect URI, FE env, BE env에 모두 같은 값으로 등록되어 있는지 확인합니다.
4. FE callback에서 `code`가 정상적으로 들어오는지 확인합니다.
5. `POST /api/auth/kakao/login`이 200을 반환하는지 확인합니다.
6. 응답의 `accessToken`으로 `GET /api/users/me`를 호출합니다.
7. `profileCompleted=false`이면 `PATCH /api/users/me/profile`로 프로필을 완료합니다.
8. 완료 후 방/장소/일정 같은 보호 API를 호출합니다.

## 12. 자주 나는 문제

| 증상 | 확인할 것 |
| --- | --- |
| Kakao `KOE006` | authorize URL의 `redirect_uri`가 Kakao Developers Redirect URI와 완전히 같은지 확인합니다. 프로토콜, 도메인, 포트, 경로, 마지막 slash까지 맞아야 합니다. |
| BE 로그인 API 403/400 | 요청 body의 `redirectUri`가 BE env `KAKAO_ALLOWED_REDIRECT_URIS`에 포함되어 있는지 확인합니다. |
| BE 로그인 API에서 Kakao token 교환 실패 | `KAKAO_CLIENT_SECRET`이 켜져 있다면 서버 env 값이 Kakao Developers의 client secret과 같은지 확인합니다. |
| CORS 에러 | FE origin이 BE env `CORS_ALLOWED_ORIGINS`에 포함되어 있는지 확인합니다. |
| 지도 SDK 로딩 실패 | FE에서 JavaScript 키를 쓰는지, JavaScript SDK 도메인에 현재 origin이 등록되어 있는지 확인합니다. |
| 장소 검색 결과가 이상함 | FE가 Kakao Local을 직접 호출하지 말고 BE의 `/api/places/search`, `/api/places/geocode`를 호출하는지 확인합니다. |
