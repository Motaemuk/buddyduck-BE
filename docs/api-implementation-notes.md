# Buddyduck API 구현 노트

이 문서는 백엔드 구현 결과를 프론트엔드/API 명세와 맞춰보기 위한 작업 노트입니다. 단순 변경 목록이 아니라, 왜 이 이슈가 생겼는지와 지금 어떤 방식으로 해결했는지를 함께 기록합니다.

## 프론트엔드 공유 요약

### Auth / Profile

- `POST /api/auth/dev-login`은 제거했습니다. 프론트는 개발 중에도 최종 흐름과 같은 `POST /api/auth/kakao/login` 기준으로 붙이는 것이 좋습니다.
- `POST /api/auth/kakao/login` 요청에는 `code`, `redirectUri`가 필요합니다. `redirectUri`는 카카오 개발자 콘솔에 등록한 값과 프론트에서 인가 코드 받을 때 사용한 값이 같아야 합니다.
- 로그인 응답에는 `profileCompleted`가 들어갑니다. 값이 `false`면 프론트에서 CB-02 프로필 완료 화면으로 보내야 합니다.
- 카카오에서 `age_range`, `gender`를 못 받으면 백엔드는 값을 비워 둡니다. 이후 `PATCH /api/users/me/profile`에서 사용자가 직접 `ageRange`, `gender`를 입력해야 가입 완료 상태가 됩니다.
- `PATCH /api/users/me/profile` 완료 요청에는 `nickname`, `ageRange`, `gender`가 필요합니다. 연령대와 성별은 서비스 안전을 위해 필수이며, 비공개 선택 필드는 사용하지 않습니다.
- API 응답에는 `profileSource`를 노출하지 않습니다. 현재 MVP에서는 출처를 API 필드로 들고 다니기보다 "카카오 기본값이 있으면 프론트에서 미리 채울 수 있고, 없으면 사용자가 직접 입력한다"는 정책 설명으로 관리합니다.

### Place

- `GET /api/places/search`, `GET /api/places/geocode`, `POST /api/places`는 모두 `Authorization: Bearer {accessToken}`이 필요합니다.
- `GET /api/places/search`와 `GET /api/places/geocode`는 `KAKAO_LOCAL_REST_API_KEY` 또는 `KAKAO_CLIENT_ID`가 있으면 Kakao Local API를 호출합니다.
- Kakao Local 키가 없으면 기존 DB 검색으로 fallback합니다. 그래서 로컬에서 키 없이도 API shape은 확인할 수 있지만, 실제 장소 검색 품질은 Kakao 키가 있어야 맞게 나옵니다.
- 검색/지오코딩 결과는 후보 목록입니다. 사용자가 장소를 선택한 뒤에는 `POST /api/places`를 호출해서 `placeId`를 받아야 방 생성, 일정 저장 같은 API에 연결할 수 있습니다.
- `GET /api/places/search`의 `concertId`, `roomId`는 명세 호환을 위해 받을 수 있지만, 현재 검색에서는 필터로 사용하지 않습니다.

### Concert / Room / Join

- `GET /api/concerts`, `GET /api/concerts/{concertId}`는 인증 없이 호출할 수 있습니다.
- `GET /api/concerts`는 기본적으로 DB cache만 조회합니다. KOPIS는 FE 요청마다 호출하지 않고, 운영 시작 전/배포 직후 1회 초기 적재 job으로 `concerts` 테이블에 저장합니다.
- 초기 적재는 KOPIS 공연 목록/상세/시설 상세를 조회해 `source=KOPIS`로 upsert합니다. 기본 범위는 오늘부터 30일 뒤까지이며, KOPIS API 특성상 실제로는 31일 범위로 이해하면 됩니다.
- KOPIS endpoint는 공식 OpenAPI 개발가이드의 `http://www.kopis.or.kr/openApi/restful`을 사용합니다. `https://www.kopis.or.kr`는 redirect 경로를 타며, 초기 적재 중 `400 Request Blocked`가 발생할 수 있어 기본값으로 쓰지 않습니다.
- KOPIS 초기 적재는 목록 1건마다 상세/시설 조회가 추가로 붙으므로 기본 `rows`를 20으로 둡니다. 상세/시설 좌표가 부족해 후보가 0건인 페이지는 기본 3번까지 건너뛴 뒤 종료합니다.
- KOPIS 연속 요청 차단을 피하기 위해 외부 요청마다 기본 100ms 대기합니다. 운영에서는 `KOPIS_REQUEST_DELAY_MILLIS`로 조정할 수 있습니다.
- KOPIS 키가 없거나 초기 적재를 실행하지 않으면 기존 DB/seed 데이터만 반환합니다. 그래서 발표 전에는 초기 적재 결과를 확인하고, 필요하면 핵심 공연을 DB에서 보정하는 것이 안전합니다.
- KOPIS 상세의 `poster`, `area`, `genrenm`, `dtguidance`는 각각 `posterUrl`, `area`, `genre`, `timeGuidance`로 저장/응답합니다. 포스터는 KOPIS 데이터에 없을 수 있어 nullable입니다.
- KOPIS의 `dtguidance`는 자유 텍스트라 항상 하나의 시작 시간으로 확정할 수 없습니다. `HH:mm` 시간이 정확히 1개만 있으면 `startAt`에 반영하고, 시간이 여러 개이거나 없으면 시작일 `00:00`으로 저장합니다. `endAt`은 종료일 `23:59:59`입니다.
- 공연 목록/상세 응답의 `openRoomCount`는 KOPIS 값이 아니라 현재 DB에 있는 `OPEN` 방 개수입니다.
- `GET /api/concerts/{concertId}/interest-tags/me`, `PUT /api/concerts/{concertId}/interest-tags/me`는 `Authorization: Bearer {accessToken}`이 필요합니다.
- 관심 태그 enum은 현재 `GOODS_BUYING`, `CAFE_VISIT`, `MEAL_TOGETHER`, `PHOTO_SPOT`, `PHOTOCARD_TRADE`, `ACCOMMODATION_SHARE`, `ENTRY_WAITING`을 사용합니다.
- `POST /api/rooms`는 요청 body의 `concertId`를 기준으로 방을 생성하며, `maxMembers`는 방장을 포함한 총 정원입니다.
- 방 생성 시 방장은 자동으로 `room_members`에 `HOST`로 들어갑니다.
- `GET /api/rooms/{roomId}/open-chat`은 방장 또는 승인된 멤버만 호출할 수 있습니다.

### Schedule

- `GET /api/rooms/{roomId}/timeline`, `POST /api/schedules/{scheduleId}/draft/recalculate`, `PUT /api/schedules/{scheduleId}/draft/commit`은 방장 또는 방 멤버만 호출할 수 있습니다.
- `GET /api/rooms/{roomId}/map`은 방장 또는 방 멤버만 호출할 수 있습니다.
- 일정 draft 미리보기는 DB에 저장하지 않고, 확정 API는 기존 slot/route를 삭제한 뒤 새 draft를 저장합니다.
- 일정 쪽은 사용자가 말한 대로 가장 섬세한 영역이라, 초과 시간 계산/모달 조건/자동 보정 같은 검증 로직은 후속 작업으로 남긴 상태입니다.

### Dev Seed

- `POST /api/dev/seed/concerts`, `POST /api/dev/seed/demo-room`은 `local`, `test` profile에서만 열립니다.
- 운영/배포 환경 프론트에서는 seed API에 의존하면 안 됩니다. 데모 데이터가 필요하면 배포 DB에 별도 seed 전략을 정해야 합니다.

## 구현된 범위

- 공연 목록/상세 조회 API를 추가했습니다.
- KOPIS 공연 목록/상세/시설 상세 조회 기반의 공연 초기 적재 job과 DB cache/upsert를 추가했습니다.
- KOPIS 포스터/지역/장르/공연시간 안내와 공연별 열린 방 수를 공연 목록/상세 응답에 추가했습니다.
- 내 공연별 관심 태그 조회/저장 API를 추가했습니다.
- 로컬/데모 데이터 생성을 위한 공연 seed API를 추가했습니다.
- 장소 검색/주소 좌표 변환/place upsert API를 추가했습니다.
- Kakao Local keyword/address 검색 연동과 DB fallback을 추가했습니다.
- 방 목록/생성/상세/내 방 목록 API를 추가했습니다.
- 가입 신청 생성/조회/승인/거절 API를 추가했습니다.
- 승인된 멤버 또는 방장만 볼 수 있는 open-chat 조회 API를 추가했습니다.
- 일정 timeline 조회 API를 추가했습니다.
- 일정 draft 미리보기/확정 API 골격과 저장 흐름을 추가했습니다.
- 방 지도 bounds 조회 API를 추가했습니다.

## 결정 및 이슈 상세

| 상태 | 항목 | 왜 발생했나 / 왜 중요한가 | 현재 해결 | 최적 방향 / 다음 액션 |
| --- | --- | --- | --- | --- |
| 완료 | Dev login 제거 | `dev-login`은 인증 우회 API라서 OAuth/JWT 흐름 검증을 흐리게 만듭니다. 프론트가 여기에 붙으면 나중에 실제 카카오 로그인으로 바꿀 때 다시 갈아엎게 됩니다. | `/api/auth/dev-login`과 관련 service/DTO/test를 제거했습니다. 없는 endpoint는 404가 나가도록 공통 예외 처리도 보강했습니다. | 앞으로 인증 테스트는 Kakao OAuth mock, 로컬 seed, 또는 test fixture로 처리합니다. |
| 완료 | Seed API profile 제한 | seed API는 데모에는 편하지만 운영에 열려 있으면 임의 데이터가 생성될 수 있습니다. | seed controller/service에 `@Profile({"local", "test"})`를 적용했습니다. | 배포 데모 데이터가 필요하면 운영 API가 아니라 DB migration, private admin, 일회성 운영 스크립트 중 하나로 분리합니다. |
| 완료 | Kakao Local 연동 | 기존 DB 검색만으로는 실제 앱의 장소 검색 UX를 검증하기 어렵습니다. 명세의 `PLACE-001/002`도 Kakao 키워드/주소 검색 성격에 가깝습니다. | Kakao Local client를 추가했고, 키가 있을 때 외부 API를 호출하도록 했습니다. 키가 없으면 DB fallback을 사용합니다. | 프론트 연동 전 로컬 `.env`에 `KAKAO_LOCAL_REST_API_KEY`를 넣고 검색 결과가 실제로 나오는지 확인합니다. |
| 완료 | KOPIS DB cache/upsert | 공연 API가 seed/수동 DB에만 의존하면 실제 공연 검색 데모가 빈약해집니다. 반대로 FE 요청마다 외부 API 결과를 그대로 반환하면 KOPIS 장애나 좌표 누락에 취약하고 응답 속도도 흔들립니다. | `GET /api/concerts`는 기본적으로 DB만 조회합니다. KOPIS 목록 → 공연 상세 → 시설 상세 조회와 `source=KOPIS` upsert는 초기 적재 job으로 분리했습니다. | `KOPIS_SERVICE_KEY`를 서버 env에 넣고, 배포 직후 `KOPIS_INITIAL_IMPORT_ENABLED=true`, `SPRING_MAIN_WEB_APPLICATION_TYPE=none`으로 1회 실행합니다. 이후 홈/검색 API는 DB cache 기준으로 동작합니다. |
| 완료 | KOPIS 공연 시간 처리 | KOPIS `dtguidance`는 `토요일(16:00,19:00)`처럼 자유 텍스트라 모든 공연을 하나의 정확한 시작 시각으로 확정할 수 없습니다. 화면에는 원문 안내가 필요하고, 정렬에는 최소한의 `startAt`이 필요합니다. | `timeGuidance` 원문을 응답에 포함합니다. `HH:mm`이 정확히 1개면 `startAt`에 반영하고, 복수/없음이면 시작일 `00:00`으로 fallback합니다. | 다회차 공연의 특정 회차를 사용자가 선택해야 한다면, 나중에 공연 회차 테이블 또는 별도 schedule option API로 분리하는 것이 맞습니다. 현재 MVP에서는 카드 표시용 원문 안내와 보수적인 fallback이 최적입니다. |
| 완료 | Kakao age/gender 미동의 처리 | 카카오는 성별/연령대 권한이 없거나 사용자가 동의하지 않으면 값을 주지 않습니다. 우리 서비스는 방장 판단에 성별/연령대가 필요해서 가입 완료 전에는 값을 받아야 합니다. | 로그인 시 값이 없으면 `NULL`로 저장합니다. 사용자가 CB-02에서 직접 입력하면 `profileCompleted=true`가 됩니다. | MVP에서는 "직접 입력 필수 + 비공개 없음"으로 확정했습니다. 나중에 카카오 권한을 받으면 기본값 prefill은 가능하지만, 최종 가입 완료에는 사용자가 값이 들어간 상태로 제출해야 합니다. |
| 완료 | 프로필 비공개 필드 제거 | 방장 판단에 필요한 연령대/성별을 비공개로 숨길 수 있으면 UX와 안전 정책이 충돌합니다. | `PRIVATE` enum과 `ageVisible`, `genderVisible` 요청/응답/DB 필드를 제거했습니다. 기존 DB의 `PRIVATE` 값은 V3 migration에서 `NULL`로 정리합니다. | 프론트 CB-02 화면에서도 비공개 chip을 제거하고, 두 필드를 필수 선택으로 처리합니다. |
| 완료 | `AUTH_REQUIRED_PROFILE_INFO` 메시지 | 기존 명세에는 성별/연령대 동의 실패 케이스가 있었지만, 현재 UX는 미동의를 로그인 실패로 막지 않습니다. 대신 프로필 미완료 상태의 서비스 API 접근을 막는 코드가 필요합니다. | `AUTH_REQUIRED_PROFILE_INFO`를 `403`으로 정리하고 메시지를 "추가 프로필 입력이 필요합니다."로 변경했습니다. | FE는 이 코드를 받으면 CB-02 추가정보 입력 화면으로 이동시키면 됩니다. |
| 완료 | Profile completion guard | 프론트가 `profileCompleted=false` 사용자를 CB-02로 보내도, 사용자가 토큰을 들고 직접 서비스 API를 호출하는 우회는 가능합니다. | `ProfileCompletionFilter`를 추가해 미완료 사용자의 보호 API 호출을 `403 AUTH_REQUIRED_PROFILE_INFO`로 차단합니다. `GET /api/users/me`, `PATCH /api/users/me/profile`, auth/health, 공개 공연 조회, Swagger/OpenAPI는 예외입니다. | FE는 로그인 응답과 `GET /api/users/me`의 `profileCompleted=false`를 먼저 처리하고, 보호 API에서 같은 코드가 오면 CB-02로 보내면 됩니다. |
| 열림 | Kakao Local 장애 fallback | Kakao Local 장애나 quota 초과가 발생하면 장소 검색 UX가 흔들릴 수 있습니다. | 현재는 키가 없을 때 DB fallback을 사용합니다. 외부 호출 실패 시 정책은 아직 별도 확정하지 않았습니다. | 운영 전 quota/error 정책을 정하고, 필요하면 장애 시 DB fallback을 명시적으로 추가합니다. |
| 열림 | Room detail 집계 범위 | 현재 room detail 응답은 명세 필수 필드 중심입니다. 화면에서 방 상세에 더 많은 집계 정보가 필요하면 프론트가 여러 API를 호출해야 할 수 있습니다. | `ROOM-003` 기본 형태를 우선 구현했습니다. | FE 화면을 붙이면서 필요한 필드를 확인한 뒤, 한 화면에서 항상 같이 쓰는 데이터만 `ROOM-003`에 확장합니다. |
| 열림 | Schedule validator 깊이 | 일정은 경로, 도보/택시, 공연 전후 시간, 모달 조건이 섞여 있어 단순 CRUD보다 오작동 위험이 큽니다. | timeline/map/draft preview/commit API와 저장 흐름은 만들었지만, 자동 일정 계산기와 세부 초과 시간 계산은 단순화되어 있습니다. | 일정 작업은 마지막에 별도 브랜치로 잡고, 화면 요구사항과 테스트 케이스를 먼저 확정한 뒤 구현합니다. |

## 구현 중 발생한 문제

| 시점 | 문제 | 원인 | 해결 |
| --- | --- | --- | --- |
| 2026-06-15 | 초기 RED compile 단계에서 `AuthService.devLogin(...)`이 없어 실패했습니다. | 당시에는 프론트 개발 편의를 위해 dev-login을 먼저 가정했습니다. | 이후 정책 변경에 따라 dev-login 전체를 제거했습니다. |
| 2026-06-15 | Place RED 테스트에서 `/api/places/search`, `/api/places/geocode`, `/api/places`가 없어 실패했습니다. | 장소 API는 명세만 있고 구현 골격이 없었습니다. | `PlaceController`, `PlaceService`, DTO, repository query를 추가했습니다. |
| 2026-06-15 | Room 테스트 이후 FK로 연결된 room 데이터가 남아 `UserControllerTest`의 `userRepository.deleteAll()`이 실패했습니다. | 테스트 cleanup 순서가 실제 FK 관계를 고려하지 않았습니다. | `RoomControllerTest`에 schedule/room/join row cleanup을 추가했습니다. |
| 2026-06-15 | Schedule RED 테스트에서 timeline, map, draft preview, commit API가 없어 실패했습니다. | 일정 모듈 API 골격이 없었습니다. | `ScheduleController`, `ScheduleService`, DTO, slot/route 저장 helper를 추가했습니다. |
| 2026-06-15 | dev-login 제거 후 없는 endpoint 요청이 공통 handler에서 500으로 처리될 수 있었습니다. | Spring의 `NoResourceFoundException`을 공통 handler에서 따로 처리하지 않았습니다. | `NoResourceFoundException`을 404로 매핑했습니다. |
| 2026-06-15 | Kakao Local client 테스트에서 query parameter 비교가 실패했습니다. | 실제 HTTP 요청에서는 한글 query가 URL encoded 값으로 전송됩니다. | 테스트 expectation을 encoded query 기준으로 수정했습니다. |

## PR 작성 메모

- 이전 BoostAD 프로젝트처럼 PR 제목은 `[Feat]`, `[Refactor]`, `[Fix]`, `[Docs]` 같은 대괄호 타입을 사용합니다.
- 커밋은 지금처럼 `feat: ...`, `docs: ...`, `refactor: ...` 형식을 유지합니다.
- 현재 PR 파이프라인에서는 `관련 이슈`와 `테스트 방법` 섹션을 넣지 않습니다.

추천 PR 본문 구조:

```md
## ✅ 작업 내용

### 📌 주요 검토 파일
- `src/main/java/...`

### ✅ 수정된 파일 요약
- ...

### 1) 배경
- 왜 이 작업이 필요했는지

### 2) 해결
- 어떤 방식으로 해결했는지

---

## 💬 To Reviewers
- 프론트/API 명세에 공유해야 할 변경점
```
