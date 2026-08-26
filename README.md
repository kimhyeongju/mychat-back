# mychat-back

Spring Boot 4.1.1 + JPA + WebSocket(STOMP) 기반 채팅 서비스 백엔드.

## 기술 스택
- Java 21 / Spring Boot 4.1.1 (Spring Framework 7, Jakarta EE 11)
- Spring Data JPA, MariaDB
- Spring Security 7 + JWT (Access/Refresh)
- WebSocket (STOMP)
- springdoc-openapi 3.x (Swagger)
- Gradle (wrapper 포함, Gradle 9.5.1)

## 패키지 구조
```
com.khj.mychatback
├── api        # 기능별 controller / dto / service
├── config     # SwaggerConfig, JpaConfig, SecurityConfig 등
├── entity.jpa # JPA 엔티티
├── enums      # 열거형
├── repo.jpa   # Spring Data JPA Repository
└── utils      # 공통 유틸
```

## 로컬 개발 실행

1. MariaDB 컨테이너만 띄우기
   ```bash
   docker compose -f docker-compose.local.yml up -d
   ```
2. IDE(또는 CLI)에서 `local` 프로필로 애플리케이션 실행
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```
3. 동작 확인
   - http://localhost:8080/api/hello
   - http://localhost:8080/swagger-ui.html

## 컨테이너 통합 실행 (앱 + DB 모두 도커)
```bash
docker compose up -d --build
curl http://localhost:8080/api/hello
```

## 프로필
| 프로필 | 용도 | DB | Redis |
|---|---|---|---|
| local | 로컬 개발 (IDE 실행) | localhost:3306 MariaDB | localhost:6379 |
| test  | GitHub Actions 테스트 | H2 인메모리 | GitHub Actions service container |
| dev   | 컨테이너 통합 확인 | docker network 내 MariaDB | docker network 내 Redis |
| prod  | 미니PC 운영 배포 | 환경변수 기반 MariaDB | 환경변수 기반 Redis |

## 인증 (JWT) 관련 API

| Method | URL | 설명 | 인증 필요 |
|---|---|---|---|
| POST | `/api/auth/phone/send-code` | 휴대폰 인증번호 발송 (알리고 SMS) | X |
| POST | `/api/auth/phone/verify` | 인증번호 검증 | X |
| POST | `/api/auth/signup` | 회원가입 (휴대폰 인증 완료 필요) | X |
| POST | `/api/auth/login` | 로그인, Access/Refresh Token 발급 | X |
| POST | `/api/auth/reissue` | Refresh Token으로 재발급 | X |
| POST | `/api/auth/logout` | Refresh Token 폐기 | O |

- Access Token은 응답 바디로 내려주며, 이후 요청은 `Authorization: Bearer {accessToken}` 헤더로 인증합니다.
- Refresh Token은 Redis에 `refresh:{username}` 키로 저장되며, 재로그인 시 이전 토큰은 자동 무효화됩니다(단일 세션).
- SMS는 `sms.aligo.mock=true`(local/test/dev 기본값)일 때 실제 발송 없이 콘솔 로그로만 인증번호를 출력합니다. 운영 배포 시 `ALIGO_API_KEY`, `ALIGO_USER_ID`, `ALIGO_SENDER` 환경변수와 `mock=false` 설정이 필요합니다.

## CI/CD
`.github/workflows/backend-ci-cd.yml`
1. `test` — 테스트 + 빌드 (모든 push/PR)
2. `docker-build-push` — Docker 이미지 빌드 후 Docker Hub push (push 이벤트)
3. `deploy` — self-hosted runner(미니PC)에서 pull & 재기동 (main 브랜치, 러너 등록 후 활성화)

### 필요한 GitHub Secrets
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

## Spring Boot 4 관련 참고
- Jackson 3가 기본이며, jjwt는 내부적으로 Jackson 2를 사용하지만 Boot 4가 Jackson 2/3을 동시 지원하는 구조라 충돌 없이 동작합니다.
- Spring Security 7부터 웹 애플리케이션이 기본적으로 보호되므로, `SecurityConfig`에 명시적인 `SecurityFilterChain` 빈이 반드시 있어야 합니다(현재 임시로 전체 permitAll).
