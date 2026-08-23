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
| 프로필 | 용도 | DB |
|---|---|---|
| local | 로컬 개발 (IDE 실행) | localhost:3306 MariaDB |
| test  | GitHub Actions 테스트 | H2 인메모리 |
| dev   | 컨테이너 통합 확인 | docker network 내 MariaDB |
| prod  | 미니PC 운영 배포 | 환경변수 기반 MariaDB |

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
