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

## 2. Gabia DNS 설정

API 서버는 프론트와 분리하기 위해 `api.boostad.site`를 사용한다.

Gabia DNS 관리 화면에서 레코드를 추가한다.

| 타입 | 호스트 | 값/위치 | TTL |
| --- | --- | --- | --- |
| A | `api` | `3.34.2.173` | 기본값 사용 |

저장 후 전파를 확인한다.

```bash
dig +short api.boostad.site
```

기대값:

```text
3.34.2.173
```

참고:

- `boostad.site` 자체를 API 서버로 쓰려면 호스트를 `@`로 둔다.
- 프론트엔드를 Vercel에 둘 계획이면 root 또는 `www`는 프론트용으로 남기고, API는 `api.boostad.site`로 분리하는 편이 깔끔하다.

## 3. EC2 배포 디렉터리 준비

EC2에 접속한다.

```bash
ssh -i ~/.ssh/concert-buddy/2026-inha-cc-10-key.pem deploy@3.34.2.173
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

PR stack을 직접 서버에서 확인해야 하는 동안에는 필요한 브랜치를 checkout한다.

```bash
git checkout chore/ec2-docker-deploy
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

KAKAO_CLIENT_ID=<kakao-rest-api-key>
KAKAO_CLIENT_SECRET=
KAKAO_LOCAL_REST_API_KEY=<kakao-rest-api-key>
```

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

## 7. 배포 중 자주 보는 문제

### DNS가 아직 EC2 IP를 가리키지 않음

확인:

```bash
dig +short api.boostad.site
```

해결:

- Gabia DNS A 레코드의 host가 `api`인지 확인한다.
- 값이 `3.34.2.173`인지 확인한다.
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
