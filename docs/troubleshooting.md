# 트러블슈팅

Concert Buddy 백엔드를 로컬 개발, 배포, FE 연동 중 자주 확인했던 문제를 정리한 문서다. 실제 secret 값은 이 문서에 적지 않는다.

## 1. 서버가 떠 있는지 먼저 확인

외부에서 API 서버가 살아 있는지 볼 때는 health API를 확인한다.

```bash
curl https://api.boostad.site/api/health
```

정상이라면 `COMMON200`과 `status: "UP"`이 온다.

서버 내부에서는 Caddy를 거치지 않고 app container 상태도 확인한다.

```bash
docker compose -f compose.prod.yml ps
docker compose -f compose.prod.yml logs --tail=100 app
docker compose -f compose.prod.yml logs --tail=100 caddy
```

## 2. CORS 오류

브라우저에서 `https://www.boostad.site` 또는 로컬 FE가 `https://api.boostad.site`를 호출할 때 CORS 오류가 나면 백엔드의 `CORS_ALLOWED_ORIGINS`를 확인한다.

운영 기준 예시:

```properties
CORS_ALLOWED_ORIGINS=https://www.boostad.site,https://boostad.site,http://localhost:5173
```

확인할 것:

- origin은 path 없이 scheme, host, port까지만 적는다.
- `https://www.boostad.site`와 `https://boostad.site`는 서로 다른 origin이다.
- Vercel preview URL에서 직접 API를 호출하면 그 preview origin도 추가해야 한다.
- Swagger UI는 `https://api.boostad.site`에서 같은 origin으로 API를 호출하므로 보통 CORS 예외가 필요하지 않다.

## 3. Kakao OAuth 오류

### KOE006 또는 redirect URI mismatch

FE가 만든 Kakao authorize URL의 `redirect_uri`, Kakao Developers에 등록한 Redirect URI, 백엔드 요청 body의 `redirectUri`, 서버 env의 `KAKAO_ALLOWED_REDIRECT_URIS`가 모두 같아야 한다.

운영에서 주로 쓰는 값:

```text
https://www.boostad.site/oauth/kakao/callback
https://boostad.site/oauth/kakao/callback
```

로컬에서 쓰는 값:

```text
http://localhost:5173/oauth/kakao/callback
```

### KOE101 앱 관리자 설정 오류

Kakao Developers에서 카카오 로그인 사용 설정, Redirect URI, JavaScript SDK 도메인, REST API 키가 맞는지 확인한다.

### 인가 code 재사용 오류

Kakao authorization code는 한 번만 사용할 수 있다. Swagger나 브라우저에서 한 번 시도한 code를 다시 보내면 token 교환이 실패할 수 있다. 새로 로그인 URL을 열어 새 code를 받아야 한다.

## 4. Kakao 키 구분

| 목적 | 값 | 사용하는 곳 |
| --- | --- | --- |
| OAuth authorize URL의 `client_id` | REST API 키 | FE |
| Kakao token 교환 | REST API 키, Client Secret | BE |
| 지도 렌더링 | JavaScript 키 | FE |
| 장소 검색/주소 좌표 변환 | REST API 키 | BE |

`KAKAO_CLIENT_SECRET`, `JWT_SECRET_KEY`, DB 비밀번호는 브라우저나 문서에 노출하지 않는다.

## 5. 프로필 미완료 에러

로그인 후 `profileCompleted=false`인 사용자는 대부분의 보호 API에서 아래 에러를 받을 수 있다.

```json
{
  "isSuccess": false,
  "code": "AUTH_REQUIRED_PROFILE_INFO",
  "message": "추가 프로필 입력이 필요합니다.",
  "result": null
}
```

FE는 이 응답을 받으면 CB-02 프로필 입력 화면으로 보내면 된다.

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

## 6. Swagger에서 JWT가 필요한 API 테스트

Swagger UI 우측 상단의 `Authorize` 버튼에 아래 형식으로 access token을 넣는다.

```text
Bearer <accessToken>
```

프로필 미완료 계정으로 보호 API를 호출하면 Swagger에서도 `AUTH_REQUIRED_PROFILE_INFO`가 날 수 있다.

## 7. 로컬 Docker MySQL이 안 뜰 때

macOS에서 Colima를 쓰는 경우 Docker daemon이 꺼져 있으면 컨테이너가 보이지 않는다.

```bash
colima start
docker ps
docker compose up -d mysql
```

로컬 MySQL은 보통 `localhost:3307`로 host에 노출한다. application env의 DB URL과 compose port가 맞는지 확인한다.

## 8. RDS를 직접 확인하고 싶을 때

RDS는 public으로 열지 않고 EC2에서만 접근하도록 두는 것이 기본이다. 로컬에서 확인하려면 SSH tunnel을 연다.

```bash
ssh -i ~/.ssh/concert-buddy/<key>.pem \
  -L 3307:<rds-endpoint>:3306 \
  ubuntu@<ec2-elastic-ip>
```

터널을 연 뒤 로컬의 DB client에서 `localhost:3307`로 접속한다. SSH 접속 프롬프트에서 `show databases;`를 입력하면 MySQL이 아니라 Ubuntu shell에 입력한 것이므로 동작하지 않는다.

## 9. KOPIS 초기 적재가 느리거나 막힐 때

KOPIS 초기 적재는 목록 API뿐 아니라 공연 상세와 시설 상세까지 추가로 호출한다. 그래서 한 달치 데이터를 가져오면 시간이 걸릴 수 있다.

확인할 로그:

```bash
docker compose -f compose.prod.yml logs -f app
```

주의할 것:

- `KOPIS_INITIAL_IMPORT_ENABLED=true`는 초기 적재 1회 실행 때만 사용한다.
- 일반 서버 실행 env에 계속 켜두지 않는다.
- 연속 요청이 차단되면 `KOPIS_REQUEST_DELAY_MILLIS`를 늘린다.
- `fetchedCount`는 목록 원본 개수가 아니라 상세/시설 조회를 통과해 DB upsert 후보가 된 개수다.

## 10. DNS와 HTTPS가 헷갈릴 때

현재 구조는 아래처럼 분리한다.

```text
www.boostad.site  -> Vercel FE
boostad.site      -> Vercel FE
api.boostad.site  -> EC2 Elastic IP -> Caddy -> Spring Boot
```

Cloudflare에서 `api` A 레코드는 EC2 Elastic IP를 바라보게 한다. 인증서 발급이나 origin 확인이 헷갈리면 잠시 `DNS only`로 바꾸고 `dig +short api.boostad.site`와 `curl https://api.boostad.site/api/health`를 확인한다.
