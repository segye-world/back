# Segye — Backend

Spring Boot 기반 REST API 서버입니다.
일정(Schedule), 할 일(Todo), 가계부(AccountRecord), 카테고리(Category) 기능을 JWT 인증으로 보호하여 제공합니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.2 |
| ORM | Spring Data JPA (Hibernate) |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Database | H2 (In-Memory, MySQL 호환 모드) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Gradle |
| 기타 | Lombok, Bean Validation |

---

## 프로젝트 구조

```
back/
├── src/main/java/com/segye/
│   ├── SegyeApplication.java       # 애플리케이션 진입점
│   ├── auth/                       # 인증 (회원가입, 로그인, JWT)
│   │   ├── AuthController.java
│   │   ├── AuthService.java
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthFilter.java
│   │   ├── JwtProperties.java
│   │   └── dto/AuthDtos.java
│   ├── member/                     # 회원 엔티티
│   │   ├── Member.java
│   │   └── MemberRepository.java
│   ├── schedule/                   # 일정 관리
│   │   ├── Schedule.java
│   │   ├── ScheduleController.java
│   │   ├── ScheduleService.java
│   │   ├── ScheduleRepository.java
│   │   └── dto/ScheduleDtos.java
│   ├── todo/                       # 할 일 관리
│   │   ├── Todo.java
│   │   ├── TodoController.java
│   │   ├── TodoService.java
│   │   ├── TodoRepository.java
│   │   └── dto/TodoDtos.java
│   ├── account/                    # 가계부 (수입/지출 내역)
│   │   ├── AccountRecord.java
│   │   ├── AccountRecordController.java
│   │   ├── AccountRecordService.java
│   │   ├── AccountRecordRepository.java
│   │   └── dto/AccountRecordDtos.java
│   ├── category/                   # 카테고리 (INCOME/EXPENSE)
│   │   ├── Category.java
│   │   ├── CategoryController.java
│   │   ├── CategoryService.java
│   │   ├── CategoryRepository.java
│   │   ├── CategoryType.java       # enum: INCOME, EXPENSE
│   │   └── dto/CategoryDtos.java
│   ├── common/                     # 공통
│   │   ├── ApiResponse.java        # 통일된 응답 형식
│   │   └── GlobalExceptionHandler.java
│   └── config/
│       ├── SecurityConfig.java     # Spring Security + CORS 설정
│       ├── SwaggerConfig.java
│       ├── H2ServerConfig.java
│       └── AppConfig.java
└── src/main/resources/
    └── application.yml
```

---

## 실행 방법

### 사전 요구사항

- Java 17 이상
- Gradle (Wrapper 포함, `./gradlew` 사용 가능)

### 실행

```bash
# Windows
gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

서버가 `http://localhost:8080` 에서 시작됩니다.

---

## API 문서 (Swagger UI)

서버 실행 후 브라우저에서 접속:

```
http://localhost:8080/swagger-ui/index.html
```

---

## H2 콘솔 (개발용 DB 확인)

```
http://localhost:8080/h2-console
```

| 항목 | 값 |
|---|---|
| JDBC URL | `jdbc:h2:mem:segye` |
| Username | `ssafy` |
| Password | `ssafy` |

> H2 In-Memory DB이므로 서버 재시작 시 데이터가 초기화됩니다.

---

## 인증 방식

JWT Bearer Token 인증을 사용합니다.

```
1. POST /api/v1/auth/signup  →  회원가입
2. POST /api/v1/auth/login   →  로그인 → accessToken 발급
3. 이후 모든 요청 Header에 추가:
   Authorization: Bearer {accessToken}
```

토큰 유효 시간: **120분** (application.yml에서 변경 가능)

---

## API 엔드포인트 목록

### Auth (인증) — 인증 불필요

| Method | URI | 설명 |
|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 (accessToken 반환) |

### Schedule (일정) — 인증 필요

| Method | URI | 설명 |
|---|---|---|
| POST | `/api/v1/schedules` | 일정 생성 |
| GET | `/api/v1/schedules?date=YYYY-MM-DD` | 날짜별 일정 조회 |
| PUT | `/api/v1/schedules/{id}` | 일정 수정 |
| DELETE | `/api/v1/schedules/{id}` | 일정 삭제 |

**Schedule DTO**
```json
// CreateRequest / UpdateRequest
{
  "title": "친구 약속",
  "date": "2025-09-09",
  "startHour": 11,
  "endHour": 15,
  "colorHex": "#F7A5A5"
}
```

### Todo (할 일) — 인증 필요

| Method | URI | 설명 |
|---|---|---|
| POST | `/api/v1/todos` | 할 일 생성 |
| GET | `/api/v1/todos?date=YYYY-MM-DD` | 날짜별 할 일 조회 |
| PUT | `/api/v1/todos/{id}` | 할 일 수정 (label, isDone) |
| DELETE | `/api/v1/todos/{id}` | 할 일 삭제 |

**Todo DTO**
```json
// CreateRequest
{
  "label": "백준 알고리즘 2문제",
  "date": "2025-09-09",
  "scheduleId": null
}

// UpdateRequest
{
  "label": "변경할 내용",
  "isDone": true
}
```

### AccountRecord (가계부) — 인증 필요

| Method | URI | 설명 |
|---|---|---|
| POST | `/api/v1/account-records` | 가계부 내역 생성 |
| GET | `/api/v1/account-records?from=&to=` | 기간별 조회 |
| GET | `/api/v1/account-records/monthly?year=&month=` | 월별 조회 |
| PUT | `/api/v1/account-records/{id}` | 수정 |
| DELETE | `/api/v1/account-records/{id}` | 삭제 |

**AccountRecord DTO**
```json
// CreateRequest / UpdateRequest
{
  "categoryId": 1,
  "amount": 12000,
  "transactionTime": "2025-09-09T12:30:00",
  "scheduleId": null
}
```

### Category (카테고리) — GET은 인증 불필요

| Method | URI | 설명 |
|---|---|---|
| GET | `/api/v1/categories?type=INCOME` | 카테고리 목록 조회 |
| POST | `/api/v1/categories` | 카테고리 생성 |
| PUT | `/api/v1/categories/{id}` | 카테고리 수정 |
| DELETE | `/api/v1/categories/{id}` | 카테고리 삭제 |

**CategoryType**: `INCOME` (수입) / `EXPENSE` (지출)

---

## 공통 응답 형식

모든 API는 아래 형식으로 응답합니다.

```json
// 성공
{
  "success": true,
  "data": { ... },
  "error": null
}

// 실패
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "email: 올바른 이메일 형식이 아닙니다"
  }
}
```

**에러 코드**

| code | HTTP | 설명 |
|---|---|---|
| `VALIDATION_ERROR` | 400 | 요청 값 검증 실패 |
| `BAD_REQUEST` | 400 | 잘못된 요청 |
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## 환경 설정 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:segye;MODE=MySQL
    username: ssafy
    password: ssafy
  jpa:
    hibernate:
      ddl-auto: update

app:
  jwt:
    secret: "CHANGE_ME_TO_A_LONG_RANDOM_SECRET"  # 배포 시 반드시 변경
    access-token-exp-minutes: 120
```

> 실제 배포 시에는 `jwt.secret`을 환경변수로 분리하고, H2 대신 MySQL/PostgreSQL을 연결하세요.

---

## 도메인 관계 요약

```
Member (회원)
  ├── Schedule (일정)         : 날짜 + 시작/종료 시간 + 색상
  ├── Todo (할 일)            : 날짜 + 완료 여부 + 일정 연결(선택)
  └── AccountRecord (가계부)  : 금액 + 거래 시각 + 카테고리 + 일정 연결(선택)

Category (카테고리)           : INCOME / EXPENSE (공통)
  └── AccountRecord에서 참조
```
