# EC2 Docker 배포 가이드

이 문서는 `buddyduck-BE`를 EC2의 Docker Compose와 Caddy로 배포하기 위한 최소 절차를 정리한다.

비밀값은 Git에 저장하지 않는다. 운영 DB 비밀번호, JWT secret, Kakao key는 EC2 서버의 `.env.prod` 또는 서버 환경변수로만 관리한다.

## 1. 서버 전제 조건

EC2 내부 기본 설정은 서버 운영 핸드북의 실행 로그를 기준으로 한다.

- 접속 사용자: `deploy`
- UFW: 22/80/443만 허용
- Docker Engine 설치 완료
- Docker Compose plugin 설치 완료
- `deploy` 사용자는 `docker` group에 포함
- Spring Boot 8080과 MySQL 3306은 public inbound로 열지 않음

AWS Security Group에서도 같은 의도를 유지한다.

| Type | Port | Source | 목적 |
| --- | --- | --- | --- |
| SSH | 22 | 내 IP 또는 팀 IP | 운영자 접속 |
| HTTP | 80 | `0.0.0.0/0`, `::/0` | Caddy HTTP challenge와 HTTPS redirect |
| HTTPS | 443 | `0.0.0.0/0`, `::/0` | API HTTPS |

열지 않는 포트:

- `8080`: Spring Boot 컨테이너는 Caddy 뒤의 Docker network 내부에서만 접근한다.
- `3306`: RDS Security Group에서 EC2 Security Group만 허용한다.

## 2. DNS 설정

API 서버는 프론트와 분리하기 위해 `api.boostad.site`를 사용한다.

DNS 관리 화면에서 레코드를 추가한다. 현재 운영은 EC2에 Elastic IP를 연결하고, `api.boostad.site`가 그 Elastic IP를 바라보게 두는 방식을 기준으로 한다.

| 타입 | 호스트 | 값/위치 | TTL |
| --- | --- | --- | --- |
| A | `api` | `<ec2-elastic-ip>` | 기본값 사용 |

저장 후 전파를 확인한다.

```bash
dig +short api.boostad.site
```

기대값:

```text
<ec2-elastic-ip>
```

참고:

- `boostad.site` 자체를 API 서버로 쓰려면 호스트를 `@`로 둔다.
- 프론트엔드를 Vercel에 둘 계획이면 root 또는 `www`는 프론트용으로 남기고, API는 `api.boostad.site`로 분리하는 편이 깔끔하다.
- Cloudflare를 사용한다면 API 레코드는 orange cloud proxy를 켜도 되지만, Caddy 인증서 발급이나 장애 원인 확인 때는 일시적으로 `DNS only`로 바꾸면 원인 분리가 쉽다.
- EC2를 중지/시작할 일이 있다면 public IP가 바뀌지 않도록 Elastic IP를 연결한다.

## 3. EC2 배포 디렉터리 준비

EC2에 접속한다.

```bash
ssh -i ~/.ssh/concert-buddy/<your-key.pem> deploy@<ec2-public-ip>
```

배포 디렉터리를 만든다.

```bash
sudo mkdir -p /opt/buddyduck
sudo chown deploy:deploy /opt/buddyduck
cd /opt/buddyduck
```

repo를 클론하거나 최신 코드를 가져온다.

```bash
git clone https://github.com/Motaemuk/buddyduck-BE.git .
```

이미 클론되어 있다면:

```bash
git pull
```

운영 배포는 임시 feature branch가 아니라 `main` 또는 release tag 기준으로 진행한다.

```bash
git checkout main
git pull --ff-only origin main
```

## 4. 운영 파일 생성

예시 파일을 실제 운영 파일로 복사한다.

```bash
cp docker-compose.prod.example.yml compose.prod.yml
cp Caddyfile.example Caddyfile
```

Caddyfile의 이메일을 실제 운영자 이메일로 바꾼다.

```caddyfile
{
	email your-email@example.com
}
```

도메인은 기본적으로 `api.boostad.site`를 사용한다.

```caddyfile
api.boostad.site {
	reverse_proxy app:8080
}
```

## 5. 서버 전용 `.env.prod` 작성

`.env.prod`는 EC2 서버에만 둔다. Git에 commit하지 않는다.

```bash
nano .env.prod
```

형식:

```properties
DB_URL=jdbc:mysql://<rds-endpoint>:3306/<database-name>?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>

JWT_SECRET_KEY=<at-least-32-byte-secret>
JWT_ACCESS_EXPIRATION=3600000

CORS_ALLOWED_ORIGINS=https://boostad.site,https://www.boostad.site

KAKAO_CLIENT_ID=<kakao-rest-api-key>
KAKAO_CLIENT_SECRET=
KAKAO_ALLOWED_REDIRECT_URIS=https://www.boostad.site/oauth/kakao/callback,https://boostad.site/oauth/kakao/callback
KAKAO_LOCAL_REST_API_KEY=<kakao-rest-api-key>

KOPIS_SERVICE_KEY=<kopis-service-key>
KOPIS_SYNC_ON_QUERY=false
KOPIS_REQUEST_DELAY_MILLIS=100
KOPIS_INITIAL_IMPORT_DAYS=30
KOPIS_INITIAL_IMPORT_ROWS=20
KOPIS_INITIAL_IMPORT_MAX_PAGES=100
KOPIS_INITIAL_IMPORT_EMPTY_PAGE_TOLERANCE=3
```

Vercel preview 도메인에서 API를 직접 호출해야 하면 `CORS_ALLOWED_ORIGINS`에 해당 preview origin을 쉼표로 추가한다.
KOPIS 기본 endpoint는 공식 OpenAPI 개발가이드의 `http://www.kopis.or.kr/openApi/restful`을 따른다. `https://www.kopis.or.kr`는 redirect 경로를 타며, 초기 적재 중 KOPIS에서 `400 Request Blocked`가 발생할 수 있다.
KOPIS 초기 적재는 공연 목록 1건마다 상세/시설 조회를 추가로 호출하므로 `KOPIS_INITIAL_IMPORT_ROWS`는 20 정도로 작게 유지한다.
KOPIS에서 연속 요청을 차단할 수 있으므로 `KOPIS_REQUEST_DELAY_MILLIS`로 외부 요청 사이에 짧은 대기 시간을 둔다.
`KOPIS_INITIAL_IMPORT_ENABLED=true`는 일반 서버 실행용 `.env.prod`에 계속 넣지 않는다. 초기 적재를 1회 실행할 때만 command 환경변수로 넘긴다.

권한을 제한한다.

```bash
chmod 600 .env.prod
```

## 6. 컨테이너 실행

이미지를 빌드하고 컨테이너를 실행한다.

```bash
docker compose -f compose.prod.yml up -d --build
```

상태를 확인한다.

```bash
docker compose -f compose.prod.yml ps
docker compose -f compose.prod.yml logs -f app
docker compose -f compose.prod.yml logs -f caddy
```

서버 내부에서 health check를 확인한다.

```bash
curl http://localhost/api/health
```

DNS와 인증서 발급 후 외부에서 확인한다.

```bash
curl https://api.boostad.site/api/health
```

KOPIS 공연 cache를 처음 채울 때는 일반 서버 컨테이너와 분리해서 1회 실행한다.

```bash
docker compose -f compose.prod.yml run --rm \
  -e KOPIS_INITIAL_IMPORT_ENABLED=true \
  -e SPRING_MAIN_WEB_APPLICATION_TYPE=none \
  app
```

실행 로그에서 `KOPIS initial import finished`와 `fetchedCount`, `syncedCount`를 확인한다. 이후 일반 서버는 그대로 DB cache를 조회한다.
`fetchedCount`는 KOPIS 목록 원본 건수가 아니라 상세/시설 조회까지 통과해 DB upsert 후보가 된 건수다.
일부 KOPIS 목록 페이지는 상세/시설 좌표가 부족해 후보가 0건일 수 있으므로, 초기 적재는 빈 후보 페이지를 `KOPIS_INITIAL_IMPORT_EMPTY_PAGE_TOLERANCE` 횟수만큼 건너뛴 뒤 종료한다.

## 7. 배포 중 자주 보는 문제

### DNS가 아직 EC2 IP를 가리키지 않음

확인:

```bash
dig +short api.boostad.site
```

해결:

- DNS A 레코드의 host가 `api`인지 확인한다.
- 값이 현재 EC2 Elastic IP인지 확인한다.
- DNS 전파까지 기다린다.

### Caddy 인증서 발급 실패

확인:

```bash
docker compose -f compose.prod.yml logs caddy
```

점검:

- Security Group에서 80/443 inbound가 public인지 확인한다.
- UFW에서 80/443이 허용되어 있는지 확인한다.
- `api.boostad.site`가 EC2 public IP를 가리키는지 확인한다.

### app 컨테이너가 unhealthy

확인:

```bash
docker compose -f compose.prod.yml logs app
```

점검:

- `.env.prod`의 DB 접속 정보가 맞는지 확인한다.
- RDS Security Group이 EC2 Security Group에서 오는 3306을 허용하는지 확인한다.
- Flyway migration 실패가 있는지 확인한다.

## 8. 중지와 재시작

재시작:

```bash
docker compose -f compose.prod.yml restart
```

내리기:

```bash
docker compose -f compose.prod.yml down
```

Caddy 인증서 volume까지 지우는 명령은 운영 중에는 신중하게 사용한다.

```bash
docker compose -f compose.prod.yml down -v
```
