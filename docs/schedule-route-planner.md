# 일정 경로 계산 설계

이 문서는 CB-11 일정 수정, CB-11' 초과 시간 경고, CB-12 지도 화면에서 사용하는 이동 구간 계산 방식을 정리한다. 이 기능은 사용자가 공연 전후 장소를 추가하고 순서를 조정할 때 "이 장소들을 실제로 소화할 수 있는가"를 판단하는 기반 기술이다.

## 1. 사용자 플로우

1. 사용자는 방 상세에서 일정 편집 화면으로 들어간다.
2. CB-11에서 장소를 추가하거나 순서를 바꾸고, 각 장소의 머무는 시간을 조정한다.
3. 장소 사이의 이동 수단은 구간별로 `WALK` 또는 `CAR_TAXI`를 선택한다.
4. FE는 `customStartAt`과 `targetArrivalAt`을 포함한 draft 요청을 보내고, BE는 각 route segment의 거리, 소요 시간, 요금 후보를 계산한다.
5. BE는 `targetArrivalAt`까지 일정을 소화하기 위해 늦어도 시작해야 하는 `recommendedStartAt`을 계산한다.
6. 사용자가 선택한 `customStartAt` 기준으로 계산 결과가 목표 도착 시간을 넘기면 CB-11'에서 초과 시간을 안내한다.
7. 저장 가능한 상태라면 사용자가 수정 완료를 누르고, BE는 사용자 시작 시간과 계산된 route segment를 DB에 저장한다.
8. CB-12 지도 화면은 저장된 슬롯 순서와 route segment를 읽어서 핀, 선, 하단 장소 정보를 보여준다.

이번 구현은 4~7번의 "이동 구간 계산과 권장 시작 시간 계산"을 실제 API/fallback 기반으로 만든다. 사용자가 추천 순서를 요청하면 BE는 같은 계산기를 이용해 저장 전 draft preview를 다시 내려준다.

## 2. 현재 구현 범위

`POST /api/schedules/{scheduleId}/draft/recalculate`, `POST /api/schedules/{scheduleId}/draft/recommend`, `PUT /api/schedules/{scheduleId}/draft/commit`에서 route segment를 계산한다.

- `customStartAt`: 사용자가 확정한 실제 일정 시작 시간이다. 없으면 기존 schedule 값, 그것도 없으면 방의 `meetingAt`을 사용한다.
- `targetArrivalAt`: 공연장 도착 목표 시간이다. 없으면 공연 시작 시간에서 `arrivalBufferMinutes`를 뺀 값을 사용한다.
- `recommendedStartAt`: 현재 슬롯/이동시간 조합을 목표 도착 시간까지 소화하려면 늦어도 시작해야 하는 서버 계산값이다.
- `fitStatus`: 사용자 시작 시간 기준으로 목표 도착 시간을 넘기면 `OVERRUN`, 아니면 `OK`다.
- `manuallyAdjusted=true`: FE가 보낸 `durationMinutes`를 그대로 사용한다.
- `manuallyAdjusted`가 없거나 `false`: from/to slot의 `placeId`로 장소 좌표를 찾고 자동 계산한다.
- Kakao Mobility 호출이 비활성화된 로컬/테스트 환경에서는 좌표 직선거리 기반 fallback 값을 사용한다.
- Kakao Mobility가 활성화된 상태에서 외부 API 호출이 실패하면 fallback으로 숨기지 않고 `SCHEDULE_ROUTE_ESTIMATION_FAILED` 에러를 반환한다.
- 장소 좌표가 없는 route segment는 기존 호환성을 위해 FE 입력 시간을 유지하되, 사용자가 직접 조정한 값과 구분되도록 `UNRESOLVED_PLACE`로 처리한다.

### 추천 순서 API

`POST /api/schedules/{scheduleId}/draft/recommend`는 사용자가 CB-11에서 "추천 순서"를 눌렀을 때 호출한다. 이 API는 DB에 저장하지 않고, 추천된 슬롯 순서와 자동 생성된 route segment를 포함한 draft preview만 반환한다. 사용자가 추천 결과를 받아들이면 FE는 응답의 `slots`, `routeSegments`를 화면 상태에 반영하고, 최종 저장 시 기존 `PUT /api/schedules/{scheduleId}/draft/commit`을 호출한다.

요청에는 `routeSegments`를 보내지 않는다. BE가 `recommendationMode` 기준으로 새 route segment를 생성한다.

```json
{
  "customStartAt": "2026-06-15T14:00:00+09:00",
  "targetArrivalAt": "2026-06-15T18:30:00+09:00",
  "arrivalBufferMinutes": 30,
  "recommendationMode": "WALK",
  "slots": [
    {
      "clientId": "slot-meeting",
      "order": 1,
      "title": "잠실역 5번 출구",
      "placeId": 10,
      "dwellMinutes": 15,
      "locked": true,
      "slotType": "MEETING",
      "category": "MEETING"
    },
    {
      "clientId": "slot-cafe",
      "order": 2,
      "title": "잠실 카페 mood",
      "placeId": 11,
      "dwellMinutes": 60,
      "locked": false,
      "slotType": "PLACE",
      "category": "CAFE_VISIT"
    }
  ]
}
```

- `recommendationMode`: 추천 순서를 계산할 기준 이동수단이다. `WALK`, `CAR_TAXI` 중 하나를 보낸다.
- 첫 번째 슬롯은 시작점으로 고정한다.
- `locked=true`인 슬롯은 현재 위치에 고정한다. 공연장 도착 블록처럼 움직이면 안 되는 블록에 사용한다.
- 나머지 슬롯은 이동시간 합이 가장 작아지도록 재배열한다.
- 이동 가능한 슬롯이 7개 이하이면 가능한 순서를 모두 비교하고, 8개 이상이면 nearest-neighbor 휴리스틱으로 계산한다.
- 추천 계산은 모든 슬롯에 `placeId`가 있어야 한다. 장소가 없는 슬롯이 있으면 `400 BAD_REQUEST`를 반환한다.
- 응답 형식은 `draft/recalculate`와 같다.

## 3. 외부 API 사용 방식

### WALK

`WALK`는 Kakao Mobility 도보 제휴 API를 직접 쓰지 않는다. 도보 API는 제휴/승인 조건이 자동차 길찾기보다 무거우므로, MVP에서는 자동차 길찾기 API로 실제 도로 이동 거리를 구한 뒤 평균 도보 속도로 시간을 추정한다.

- 공식 문서: [Driving Directions](https://developers.kakaomobility.com/guide/navi-api/directions)
- 요청 방식: `GET /v1/directions`
- 주요 요청값:
  - `origin`: `lng,lat`
  - `destination`: `lng,lat`
  - `priority=RECOMMEND`
  - `summary=true`
- 사용하는 응답값:
  - `routes[0].summary.distance`: 총 차량 경로 거리, meter

BE는 `distance / 60m`를 올림해서 `durationMinutes`로 내려준다. 즉 도보 속도를 약 3.6km/h로 보고, 콘서트 당일 혼잡과 초행길을 고려해 보수적으로 잡는다. 응답의 `provider`는 `DRIVING_DISTANCE_WALK_ESTIMATE`다.

### CAR_TAXI

`CAR_TAXI`는 Kakao Mobility Driving Directions API를 우선 사용한다.

- 공식 문서: [Driving Directions](https://developers.kakaomobility.com/guide/navi-api/directions)
- 가격/쿼터 문서: [Kakao Mobility 가격 및 문의](https://developers.kakaomobility.com/price/)
- 요청 방식: `GET /v1/directions`
- 주요 요청값:
  - `origin`: `lng,lat`
  - `destination`: `lng,lat`
  - `priority=RECOMMEND`
  - `summary=true`
- 사용하는 응답값:
  - `routes[0].summary.distance`: 총 차량 이동 거리, meter
  - `routes[0].summary.duration`: 총 차량 이동 시간, second
  - `routes[0].summary.fare.taxi`: 예상 택시요금, KRW
  - `routes[0].summary.fare.toll`: 예상 통행료, KRW

택시요금은 Kakao Driving Directions 응답값이 있을 때만 제공한다. fallback 계산에서는 요금을 만들지 않는다.

## 4. 무료/쿼터 판단

Kakao Mobility Driving Directions는 문서상 자동차 길찾기 일일 무료 제공량이 있다. 무료 제공량 초과분은 유료 정책을 따른다.

Walking Directions API는 별도 제휴/승인 흐름을 전제로 하므로 MVP에서는 사용하지 않는다. 도보 구간도 Driving Directions의 거리값을 기반으로 시간만 추정한다.

## 5. fallback 계산

fallback은 Kakao Mobility 키가 비어 있는 로컬/테스트 환경에서만 사용한다. 운영처럼 Kakao Mobility 키가 활성화된 상태에서 API 호출이 실패하면 자동으로 직선거리 추정을 사용하지 않고 `SCHEDULE_ROUTE_ESTIMATION_FAILED` 에러를 반환한다.

| 모드 | 거리 계산 | 시간 계산 | 요금 |
| --- | --- | --- | --- |
| `WALK` | 좌표 직선거리 * 1.25 | 60m/분(약 3.6km/h) 기준 올림 | 없음 |
| `CAR_TAXI` | 좌표 직선거리 * 1.35 | 평균 22km/h 기준 올림 | 없음 |

fallback은 정확한 경로 탐색이 아니라 로컬 개발과 자동 테스트의 화면 흐름을 유지하기 위한 추정값이다. 응답의 `provider`가 `FALLBACK_STRAIGHT_LINE`이면 FE에서 "예상값"으로 표현하는 것이 좋다.

## 6. FE 요청/응답에서 달라진 부분

### 요청

```json
{
  "customStartAt": "2026-06-15T14:00:00+09:00",
  "targetArrivalAt": "2026-06-15T18:30:00+09:00",
  "arrivalBufferMinutes": 30,
  "slots": [
    {
      "clientId": "slot-meeting",
      "order": 1,
      "title": "잠실역 5번 출구",
      "placeId": 10,
      "dwellMinutes": 15,
      "slotType": "MEETING",
      "category": "MEETING"
    }
  ],
  "routeSegments": [
    {
      "fromClientId": "slot-meeting",
      "toClientId": "slot-cafe",
      "mode": "WALK",
      "durationMinutes": 18,
      "manuallyAdjusted": false
    }
  ]
}
```

`manuallyAdjusted`는 optional이다.

- 생략 또는 `false`: BE가 장소 좌표 기반으로 다시 계산한다.
- `true`: 사용자가 이동 시간을 직접 조정한 상태로 보고 `durationMinutes`를 그대로 사용한다.

### draft 응답

```json
{
  "fitStatus": "OK",
  "recommendedStartAt": "2026-06-15T14:02:00+09:00",
  "effectiveStartAt": "2026-06-15T14:00:00+09:00",
  "targetArrivalAt": "2026-06-15T15:00:00+09:00",
  "overrunMinutes": 0,
  "spareMinutes": 2,
  "slots": [
    {
      "clientId": "slot-meeting",
      "startAt": "2026-06-15T14:00:00+09:00",
      "endAt": "2026-06-15T14:10:00+09:00"
    }
  ],
  "routeSegments": [
    {
      "fromClientId": "slot-meeting",
      "toClientId": "slot-cafe",
      "mode": "CAR_TAXI",
      "distanceMeters": 1033,
      "durationMinutes": 5,
      "taxiFareWon": 3800,
      "tollFareWon": 0,
      "provider": "KAKAO_DRIVING",
      "manuallyAdjusted": false
    }
  ]
}
```

`WALK` 또는 fallback에서는 `taxiFareWon`, `tollFareWon`이 비어 있을 수 있다.

`WALK` 자동 계산 응답은 아래처럼 내려온다.

```json
{
  "fromClientId": "slot-meeting",
  "toClientId": "slot-cafe",
  "mode": "WALK",
  "distanceMeters": 1261,
  "durationMinutes": 22,
  "taxiFareWon": null,
  "tollFareWon": null,
  "provider": "DRIVING_DISTANCE_WALK_ESTIMATE",
  "manuallyAdjusted": false
}
```

### timeline/map 응답

`GET /api/rooms/{roomId}/timeline`, `GET /api/rooms/{roomId}/map`의 `routeSegments`에도 아래 필드가 포함된다.

`GET /api/rooms/{roomId}/timeline`의 `schedule`에는 아래 필드가 포함된다.

- `customStartAt`: 저장된 사용자 시작 시간
- `targetArrivalAt`: 저장된 목표 도착 시간
- `recommendedStartAt`: 저장된 슬롯/이동시간 기준 권장 시작 시간
- `overrunMinutes`: 저장된 사용자 시작 시간 기준 초과 시간
- `spareMinutes`: 저장된 사용자 시작 시간 기준 여유 시간

`routeSegments`에는 아래 필드가 포함된다.

- `distanceMeters`
- `durationMinutes`
- `taxiFareWon`
- `tollFareWon`
- `provider`
- `manuallyAdjusted`

## 7. 서버 설정값

운영 서버에서는 아래 값을 `.env.prod` 또는 서버 환경변수로 관리한다. 실제 값은 문서나 Git에 쓰지 않는다.

```env
KAKAO_MOBILITY_REST_API_KEY=<카카오 REST API 키>
```

현재 애플리케이션 설정은 `KAKAO_MOBILITY_REST_API_KEY`가 없으면 `KAKAO_LOCAL_REST_API_KEY`를 fallback으로 사용한다. OAuth `KAKAO_CLIENT_ID`는 Mobility API 인증키와 계약이 다르므로 fallback으로 사용하지 않는다.

## 8. 남은 확장 여지

현재 추천 순서는 이동시간 합이 가장 짧은 순서를 찾는 데 집중한다. 장소 수가 적은 MVP에서는 완전 탐색이 단순하고 설명 가능하며, 장소 수가 많아지면 nearest-neighbor 휴리스틱으로 응답 시간을 지킨다.

이후 더 정교하게 만들 수 있는 부분은 아래와 같다.

1. `WALK`와 `CAR_TAXI`를 섞어서 추천하는 혼합 이동수단 추천.
2. 장소별 영업시간, 대기시간, 공연장 입장 마감 같은 시간 제약 반영.
3. 이동시간뿐 아니라 사용자의 관심 태그, 선호 장소, 비용까지 고려한 가중치 추천.
4. 초과 시간이 발생했을 때 "어떤 장소를 줄이거나 택시로 바꾸면 되는지"를 설명하는 보조 메시지.

AI는 최적 경로 계산의 주체로 쓰기보다, 계산 결과를 사람이 이해하기 쉽게 설명하거나 대안을 요약하는 보조 역할이 적합하다.
