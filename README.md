# khj-playground-back

[khj-playground.site](https://khj-playground.site)의 백엔드입니다.
한 도메인에서 여러 서비스(채팅 / 게시판 / 모의투자)를 제공하기 위해 Gradle 멀티모듈로 구성했습니다.

## 기술 스택

| 분류          | 사용 기술                                                      |
| ------------- | -------------------------------------------------------------- |
| 언어 / 런타임 | Java 21                                                        |
| 프레임워크    | Spring Boot 4.1.1, Spring Security 7                           |
| 영속성        | Spring Data JPA (Hibernate 7), MariaDB 11                      |
| 스키마 관리   | Flyway 12                                                      |
| 캐시 / 세션   | Redis 7                                                        |
| 빌드          | Gradle (멀티모듈)                                              |
| 문서화        | springdoc-openapi (Swagger UI)                                 |
| 외부 연동     | SOLAPI (SMS 인증)                                              |
| 배포          | Docker, GitHub Actions (self-hosted runner), Cloudflare Tunnel |

## 모듈 구조

```
khj-playground-back/
├── common/   공통 엔티티·예외·모듈 간 계약 인터페이스
├── auth/     회원, 인증, JWT, SMS 인증
├── board/    게시판, 글, 댓글, 첨부파일
├── chat/     (예정)
├── invest/   (예정)
└── app/      부트스트랩. 유일한 실행 모듈
```

### 의존 방향

```
app → (board, chat, invest) → auth → common
```

한 방향으로만 흐르며, **서비스 모듈끼리는 서로 참조하지 않습니다.** Gradle이 컴파일 타임에 이를 강제합니다.

서비스 모듈이 회원 정보를 알아야 할 때는 `auth`의 `User` 엔티티를 직접 참조하지 않고, `common`에 정의된 계약을 거칩니다.

```java
// common/security/CurrentUserProvider.java
public interface CurrentUserProvider {
  AuthenticatedUser resolve(Authentication authentication);
}
```

`board`는 작성자를 `UUID authorId` + 닉네임 스냅샷으로만 저장합니다. 나중에 모듈을 별도 서비스로 분리하거나 DB를 나눌 때 걸림돌이 되지 않도록 한 설계입니다.

### 패키지 배치

모듈은 나뉘어 있지만 패키지 루트는 공유합니다. `@SpringBootApplication`이 `com.khj.playground`에 있어 하위 모듈이 전부 컴포넌트 스캔 범위에 들어오므로, `@EntityScan`이나 `@EnableJpaRepositories`를 따로 선언하지 않습니다.

```
app     : com/khj/playground/PlaygroundApplication.java
common  : com/khj/playground/common/...
auth    : com/khj/playground/auth/...
board   : com/khj/playground/board/...
```

## 로컬 실행

### 사전 준비

- JDK 21
- Docker Desktop

### 1. 인프라 컨테이너 기동

```bash
docker compose -f docker-compose.local.yml up -d
```

MariaDB와 Redis만 컨테이너로 띄우고 애플리케이션은 IDE나 Gradle로 직접 실행합니다.

> 호스트에 네이티브 MariaDB(3306) / Redis(6379)가 설치되어 있는 경우를 피하기 위해
> 로컬 컨테이너는 **3307 / 6380**으로 매핑합니다.

### 2. 애플리케이션 실행

```bash
./gradlew :app:bootRun --args='--spring.profiles.active=local'
```

멀티모듈이므로 `bootRun`이 아니라 `:app:bootRun`입니다.

### 3. 확인

```bash
curl http://localhost:8080/api/hello
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- 우측 상단 **Authorize**에 로그인으로 받은 accessToken을 입력하면 인증이 필요한 API를 호출할 수 있습니다. (`Bearer ` 접두사 없이 토큰만)

### 스키마를 초기화하고 싶을 때

```bash
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d
```

`-v`로 볼륨까지 지워야 Flyway가 처음부터 다시 적용됩니다.

## 프로필

| 프로필  | DB                      | SMS              | 용도              |
| ------- | ----------------------- | ---------------- | ----------------- |
| `local` | localhost:3307 컨테이너 | Mock (콘솔 출력) | 일상 개발         |
| `dev`   | 컨테이너 네트워크       | Mock             | 배포 전 통합 검증 |
| `prod`  | 컨테이너 네트워크       | SOLAPI 실발송    | 운영              |

SMS 구현체는 설정값이 아니라 `@Profile`로 갈립니다. `prod`가 아니면 `MockSmsSender`만 빈으로 등록되므로, 개발 중에 문자 비용이 발생하지 않습니다.

`dev` 프로필 통합 실행은 운영과 가장 가까운 형태라 배포 전 검증에 씁니다.

```bash
docker compose up -d --build
docker compose logs -f backend
```

## 데이터베이스

### Flyway

스키마 변경은 전부 마이그레이션 스크립트로 관리하며, Hibernate는 `ddl-auto: validate`로 검증만 수행합니다.

| 모듈   | 버전 대역 | 위치                                         | 테이블 접두어 |
| ------ | --------- | -------------------------------------------- | ------------- |
| auth   | V1xx      | `auth/src/main/resources/db/migration/auth/` | `users`       |
| chat   | V2xx      | `chat/.../db/migration/chat/`                | `chat_`       |
| board  | V3xx      | `board/.../db/migration/board/`              | `board_`      |
| invest | V4xx      | `invest/.../db/migration/invest/`            | `invest_`     |

모듈별로 버전 대역을 나눈 이유는 여러 모듈이 동시에 같은 번호를 쓰는 충돌을 막기 위해서입니다.

**이미 적용된 마이그레이션 파일은 수정하지 않습니다.** Flyway가 체크섬을 저장하므로 내용이 바뀌면 부팅이 실패합니다. 변경이 필요하면 새 버전 파일을 추가하세요.

파일명 규칙은 `V{버전}__{설명}.sql`이며, **밑줄이 두 개**입니다.

### 주요 테이블

```
users              회원
board_boards       게시판 (자유게시판, 공지사항)
board_posts        글
board_comments     댓글
board_attachments  첨부파일 메타데이터
```

## 인증

JWT 기반이며 access token과 refresh token을 사용합니다. refresh token은 Redis에 저장해 폐기가 가능하도록 했습니다.

### 공개 엔드포인트 등록 방식

`SecurityConfig`가 다른 모듈의 URL을 알지 않도록, 각 모듈이 자신의 공개 경로를 직접 등록합니다.

```java
// board/config/BoardPublicEndpoints.java
@Component
public class BoardPublicEndpoints implements PublicEndpoints {
  @Override
  public List<PublicEndpoint> endpoints() {
    return List.of(
      PublicEndpoint.get("/api/boards"),
      PublicEndpoint.get("/api/boards/*/posts")
      // GET만 등록 → POST/PATCH/DELETE는 자동으로 인증 필수
    );
  }
}
```

새 모듈을 추가할 때 `SecurityConfig`를 수정할 필요가 없습니다.

### SMS 인증

SOLAPI 개인 계정을 사용합니다. 일일 발송 한도가 있어 Redis로 남용을 제한합니다.

- 같은 번호 재요청: 60초 쿨다운
- 번호당 하루 요청: 5회

## API 개요

전체 명세는 Swagger UI를 참고하세요.

### 인증 (`/api/auth`)

| 메서드 | 경로               | 인증 | 설명               |
| ------ | ------------------ | ---- | ------------------ |
| POST   | `/phone/send-code` | —    | 인증번호 발송      |
| POST   | `/phone/verify`    | —    | 인증번호 확인      |
| POST   | `/signup`          | —    | 회원가입           |
| POST   | `/login`           | —    | 로그인             |
| POST   | `/reissue`         | —    | 토큰 재발급        |
| POST   | `/logout`          | 필요 | 로그아웃           |
| GET    | `/me`              | 필요 | 내 정보 조회       |
| PATCH  | `/me`              | 필요 | 닉네임·이메일 수정 |
| DELETE | `/withdraw`        | 필요 | 회원 탈퇴          |

### 게시판 (`/api/boards`, `/api/posts`)

| 메서드 | 경로                                | 인증   | 설명                   |
| ------ | ----------------------------------- | ------ | ---------------------- |
| GET    | `/api/boards`                       | —      | 게시판 목록            |
| GET    | `/api/boards/{slug}/posts`          | —      | 글 목록 (페이징, 검색) |
| GET    | `/api/boards/{slug}/posts/{postId}` | —      | 글 상세                |
| POST   | `/api/boards/{slug}/posts`          | 필요   | 글 작성                |
| PATCH  | `/api/boards/{slug}/posts/{postId}` | 작성자 | 글 수정                |
| DELETE | `/api/boards/{slug}/posts/{postId}` | 작성자 | 글 삭제                |
| GET    | `/api/posts/{postId}/comments`      | —      | 댓글 목록              |
| POST   | `/api/posts/{postId}/comments`      | 필요   | 댓글 작성              |
| POST   | `/api/posts/{postId}/attachments`   | 작성자 | 파일 첨부              |
| GET    | `/api/attachments/{id}`             | —      | 파일 다운로드          |

읽기는 비로그인도 가능하고 쓰기는 로그인이 필요합니다. 공지사항 게시판(`admin_only`)은 관리자만 글을 쓸 수 있습니다.

### 조회수 중복 방지

글 조회 시 Redis에 `board:post:{id}:viewer:{식별자}` 키를 1시간 TTL로 남깁니다. `SET NX EX` 한 번으로 처리하므로 동시 요청에도 중복 증가가 없습니다. Redis 장애 시에는 조회수만 포기하고 글은 정상 조회됩니다.

## 파일 첨부

- 저장 위치: 컨테이너 `/app/uploads` (Docker 볼륨 마운트)
- 파일명: UUID로 저장하고 원본 이름은 DB에만 보관 (경로 조작 방지)
- 허용 확장자: 화이트리스트 방식 (이미지, 문서, 압축 파일)
- 제한: 글당 5개, 개당 10MB

## 배포

`main` 브랜치 push 시 GitHub Actions가 자동 배포합니다.

```
push → 테스트 → Docker 이미지 빌드/푸시 → self-hosted runner에서 pull & 재기동
```

운영 환경은 미니PC에서 Docker Compose로 동작하며, 외부 접근은 Cloudflare Tunnel 하나만 거칩니다. DB와 Redis는 호스트에 포트를 노출하지 않습니다.

```
Cloudflare Tunnel
  ├─ /api, /ws, /swagger-ui  → backend:8080
  └─ 그 외                    → frontend:80
```

프론트와 백엔드가 같은 오리진을 사용하므로 CORS 설정이 없습니다.

### 운영 환경변수

`/opt/khj-playground/.env`

```
DOCKERHUB_USERNAME=
DB_ROOT_PASSWORD=
DB_NAME=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
SOLAPI_API_KEY=
SOLAPI_API_SECRET=
SOLAPI_SENDER=
```

## 새 서비스 모듈 추가 절차

`board` 모듈이 기준 사례입니다.

1. `settings.gradle`에 `include` 추가
2. `{module}/build.gradle` 작성 (`implementation project(':auth')`)
3. `app/build.gradle`에 `implementation project(':{module}')` 추가
4. `application.yml`의 `flyway.locations`에 경로 추가
5. 마이그레이션 작성 (모듈 버전 대역 준수, 테이블 접두어 사용)
6. 엔티티는 `UUID authorId`로 회원 참조
7. `{Module}PublicEndpoints`로 공개 경로 등록
8. 컨트롤러의 `@PathVariable`, `@RequestParam`에 이름 명시

## 자주 겪는 문제

**`./gradlew bootRun`에서 `Task 'bootRun' not found`**
멀티모듈이므로 `:app:bootRun`을 사용하세요.

**`Unable to delete directory .../build`**
실행 중인 애플리케이션이 jar를 잡고 있습니다. 앱을 종료하고 `./gradlew --stop` 후 재시도하세요.

**IDE가 삭제한 클래스를 계속 로드함**
VS Code가 만든 `bin/` 디렉터리가 클래스패스에 섞인 경우입니다.

```bash
rm -rf bin */bin build */build
./gradlew clean build
```

**`Name for argument ... not specified`**
`@PathVariable("name")`, `@RequestParam(name = "...")`, `@Param("...")`으로 이름을 명시하세요.
