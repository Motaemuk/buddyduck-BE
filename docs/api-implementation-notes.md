# Buddyduck API 구현 노트

백엔드 API를 구현하면서 프론트엔드와 공유해야 하거나, 이후 결정이 필요한 내용을 기록합니다.

## 구현된 범위

- 공연 목록/상세 조회 API를 추가했습니다.
- 내 공연별 관심 태그 조회/저장 API를 추가했습니다.
- 로컬/데모 데이터 생성을 위한 공연 seed API를 추가했습니다.
- 장소 검색/주소 좌표 변환/place upsert API를 추가했습니다.

## 프론트엔드 공유 필요

- `GET /api/concerts`, `GET /api/concerts/{concertId}`는 인증 없이 호출할 수 있습니다.
- `GET /api/concerts/{concertId}/interest-tags/me`, `PUT /api/concerts/{concertId}/interest-tags/me`는 `Authorization: Bearer {accessToken}`이 필요합니다.
- 공연 시간 응답은 `2026-06-15T19:00:00+09:00` 형태의 KST offset 문자열로 내려갑니다.
- 관심 태그 enum은 현재 `GOODS_BUYING`, `CAFE_VISIT`, `MEAL_TOGETHER`, `PHOTO_SPOT`, `PHOTOCARD_TRADE`, `ACCOMMODATION_SHARE`, `ENTRY_WAITING`을 사용합니다.
- `POST /api/dev/seed/concerts`는 로컬/데모용 임시 API입니다. 운영 배포 전 제거하거나 접근 제한을 걸어야 합니다.
- `GET /api/places/search`, `GET /api/places/geocode`, `POST /api/places`는 모두 `Authorization: Bearer {accessToken}`이 필요합니다.
- `GET /api/places/search`, `GET /api/places/geocode`는 현재 외부 Kakao Local 실시간 호출이 아니라 DB에 저장된 place 후보를 조회합니다.
- `GET /api/places/search`의 `concertId`, `roomId`는 명세 호환을 위해 받을 수 있지만, 현재 DB 후보 검색에서는 필터로 사용하지 않습니다.
- `POST /api/places`는 프론트가 선택한 장소를 서버 DB에 upsert할 때 사용합니다.

## 남은 결정

| 항목 | 왜 생각해야 하나 | 선택지 | 현재 권장 |
| --- | --- | --- | --- |
| seed API 운영 노출 | seed API가 운영에서 열리면 데모 데이터를 임의로 만들 수 있습니다. | 완전 제거, local/test profile에서만 활성화, 관리자용 secret/header 추가 | 운영 배포 전 제거하거나 local/test profile 전용으로 제한 |
| 관심 태그 enum 확장 | 프론트 UI 칩과 백엔드 enum이 다르면 저장 시 400이 발생합니다. | 현재 enum 유지, API 명세와 UI 칩을 함께 수정 | API 명세를 기준으로 FE/BE enum을 같이 관리 |
| Kakao Local 실시간 연동 | 현재 place API는 이름상 Kakao Local 검색처럼 보이지만, 실제로는 로컬 DB 후보를 조회합니다. FE가 실시간 검색으로 기대하면 결과 범위가 제한됩니다. | DB 후보 조회 유지, Kakao Local adapter 추가, FE에서 직접 Kakao Local 호출 | 백엔드 adapter를 추가하되 API key/쿼터/장애 시 fallback 정책을 정한 뒤 적용 |
