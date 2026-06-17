# Buddyduck API 구현 노트

이 문서는 백엔드 구현 결과를 프론트엔드/API 명세와 맞춰보기 위한 작업 노트입니다. 단순 변경 목록이 아니라, 왜 이 이슈가 생겼는지와 지금 어떤 방식으로 해결했는지를 함께 기록합니다.

## 프론트엔드 공유 요약

### Auth / Profile

- `POST /api/auth/dev-login`은 제거했습니다. 프론트는 개발 중에도 최종 흐름과 같은 `POST /api/auth/kakao/login` 기준으로 붙이는 것이 좋습니다.
- `POST /api/auth/kakao/login` 요청에는 `code`, `redirectUri`가 필요합니다. `redirectUri`는 카카오 개발자 콘솔에 등록한 값과 프론트에서 인가 코드 받을 때 사용한 값이 같아야 합니다.
- 로그인 응답에는 `profileCompleted`가 들어갑니다. 값이 `false`면 프론트에서 CB-02 프로필 완료 화면으로 보내야 합니다.
- 카카오에서 `age_range`, `gender`를 못 받으면 백엔드는 `PRIVATE`로 저장합니다. 이후 `PATCH /api/users/me/profile`에서 사용자가 직접 `ageRange`, `gender`를 입력해야 가입 완료 상태가 됩니다.
- `PATCH /api/users/me/profile` 완료 요청에서는 `ageRange=PRIVATE`, `gender=PRIVATE`를 허용하지 않습니다. 화면에서는 비공개 여부를 `ageVisible=false`, `genderVisible=false`로 표현해야 합니다.
- API 응답에는 `profileSource`를 노출하지 않습니다. 현재 MVP에서는 출처를 API 필드로 들고 다니기보다 "카카오 기본값이 있으면 프론트에서 미리 채울 수 있고, 없으면 사용자가 직접 입력한다"는 정책 설명으로 관리합니다.

### Place

- `GET /api/places/search`, `GET /api/places/geocode`, `POST /api/places`는 모두 `Authorization: Bearer {accessToken}`이 필요합니다.
- `GET /api/places/search`와 `GET /api/places/geocode`는 `KAKAO_LOCAL_REST_API_KEY` 또는 `KAKAO_CLIENT_ID`가 있으면 Kakao Local API를 호출합니다.
- Kakao Local 키가 없으면 기존 DB 검색으로 fallback합니다. 그래서 로컬에서 키 없이도 API shape은 확인할 수 있지만, 실제 장소 검색 품질은 Kakao 키가 있어야 맞게 나옵니다.
- 검색/지오코딩 결과는 후보 목록입니다. 사용자가 장소를 선택한 뒤에는 `POST /api/places`를 호출해서 `placeId`를 받아야 방 생성, 일정 저장 같은 API에 연결할 수 있습니다.
- `GET /api/places/search`의 `concertId`, `roomId`는 명세 호환을 위해 받을 수 있지만, 현재 검색에서는 필터로 사용하지 않습니다.

### Concert / Room / Join

- `GET /api/concerts`, `GET /api/concerts/{concertId}`는 인증 없이 호출할 수 있습니다.
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
- 내 공연별 관심 태그 조회/저장 API를 추가했습니다.
- 로컬/데모 데이터 생성을 위한 공연 seed API를 추가했습니다.
- 장소 검색/주소 좌표 변환/place upsert API를 추가했습니다.
- Kakao Local keyword/address 검색 연동과 DB fallback을 추가했습니다.
- 방 목록/생성/상세/내 방 목록 API를 추가했습니다.
- 가입 신청 생성/조회/승인/거절 API를 추가했습니다.
- 승인된 멤버 또는 방장만 볼 수 있는 open-chat 조회 API를 추가했습니다.
- 일정 timeline 조회 API를 추가했습니다.
- 일정 draft 미리보기/확정 API를 추가했습니다.
- 방 지도 bounds 조회 API를 추가했습니다.

## 결정 및 이슈 상세

| 상태 | 항목 | 왜 발생했나 / 왜 중요한가 | 현재 해결 | 최적 방향 / 다음 액션 |
| --- | --- | --- | --- | --- |
| 완료 | Dev login 제거 | `dev-login`은 인증 우회 API라서 OAuth/JWT 흐름 검증을 흐리게 만듭니다. 프론트가 여기에 붙으면 나중에 실제 카카오 로그인으로 바꿀 때 다시 갈아엎게 됩니다. | `/api/auth/dev-login`과 관련 service/DTO/test를 제거했습니다. 없는 endpoint는 404가 나가도록 공통 예외 처리도 보강했습니다. | 앞으로 인증 테스트는 Kakao OAuth mock, 로컬 seed, 또는 test fixture로 처리합니다. |
| 완료 | Seed API profile 제한 | seed API는 데모에는 편하지만 운영에 열려 있으면 임의 데이터가 생성될 수 있습니다. | seed controller/service에 `@Profile({"local", "test"})`를 적용했습니다. | 배포 데모 데이터가 필요하면 운영 API가 아니라 DB migration, private admin, 일회성 운영 스크립트 중 하나로 분리합니다. |
| 완료 | Kakao Local 연동 | 기존 DB 검색만으로는 실제 앱의 장소 검색 UX를 검증하기 어렵습니다. 명세의 `PLACE-001/002`도 Kakao 키워드/주소 검색 성격에 가깝습니다. | Kakao Local client를 추가했고, 키가 있을 때 외부 API를 호출하도록 했습니다. 키가 없으면 DB fallback을 사용합니다. | 프론트 연동 전 로컬 `.env`에 `KAKAO_LOCAL_REST_API_KEY`를 넣고 검색 결과가 실제로 나오는지 확인합니다. |
| 완료 | Kakao age/gender 미동의 처리 | 카카오는 성별/연령대 권한이 없거나 사용자가 동의하지 않으면 값을 주지 않습니다. 우리 서비스는 방장 판단에 성별/연령대가 필요해서 가입 완료 전에는 값을 받아야 합니다. | 로그인 시 값이 없으면 `PRIVATE`로 저장합니다. 사용자가 CB-02에서 직접 입력하면 `profileCompleted=true`가 됩니다. | MVP에서는 "직접 입력 필수 + 공개 여부 선택"이 가장 안정적입니다. 나중에 카카오 권한을 받으면 기본값 prefill만 하고, 사용자가 비공개 여부를 선택하게 합니다. |
| 열림 | `AUTH_REQUIRED_PROFILE_INFO` 메시지 | 기존 명세에는 성별/연령대 동의 실패 케이스가 있었지만, 현재 UX는 미동의를 로그인 실패로 막지 않습니다. | Kakao 응답 구조가 깨졌거나 지원하지 않는 값이면 `AUTH_REQUIRED_PROFILE_INFO`가 날 수 있습니다. 일반적인 age/gender 미동의는 `PRIVATE` 저장 후 CB-02로 보냅니다. | 에러 메시지를 "카카오 프로필 정보를 확인할 수 없습니다."처럼 넓은 의미로 조정하거나, 실제 age/gender 미동의 실패 정책을 다시 채택할 때만 현재 메시지를 유지합니다. |
| 열림 | Profile completion guard | 프론트가 `profileCompleted=false` 사용자를 CB-02로 보내도, 사용자가 토큰을 들고 직접 서비스 API를 호출하는 우회는 가능합니다. | 이번에는 guard를 넣지 않았습니다. 프론트 플로우에서 먼저 막는 상태입니다. | 구현량은 크지 않습니다. 인증이 안정화된 뒤 `PATCH /api/users/me/profile`, `GET /api/users/me`, auth/health 정도만 예외로 두고 나머지를 막는 interceptor/filter를 추가하는 것이 좋습니다. |
| 열림 | Kakao Local 장애 fallback | Kakao Local 장애나 quota 초과가 발생하면 장소 검색 UX가 흔들릴 수 있습니다. | 현재는 키가 없을 때 DB fallback을 사용합니다. 외부 호출 실패 시 정책은 아직 별도 확정하지 않았습니다. | 운영 전 quota/error 정책을 정하고, 필요하면 장애 시 DB fallback을 명시적으로 추가합니다. |
| 열림 | Room detail 집계 범위 | 현재 room detail 응답은 명세 필수 필드 중심입니다. 화면에서 방 상세에 더 많은 집계 정보가 필요하면 프론트가 여러 API를 호출해야 할 수 있습니다. | `ROOM-003` 기본 형태를 우선 구현했습니다. | FE 화면을 붙이면서 필요한 필드를 확인한 뒤, 한 화면에서 항상 같이 쓰는 데이터만 `ROOM-003`에 확장합니다. |
| 열림 | Schedule validator 깊이 | 일정은 경로, 도보/택시, 공연 전후 시간, 모달 조건이 섞여 있어 단순 CRUD보다 오작동 위험이 큽니다. | draft preview/commit 구조와 저장 흐름은 만들었지만, 세부 초과 시간 계산은 단순화되어 있습니다. | 일정 작업은 마지막에 별도 브랜치로 잡고, 화면 요구사항과 테스트 케이스를 먼저 확정한 뒤 구현합니다. |

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
