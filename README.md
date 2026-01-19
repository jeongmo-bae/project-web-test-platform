# Web Test Platform

JUnit 5 Platform 기반의 웹 테스트 자동화 플랫폼입니다. 웹 UI에서 테스트를 발견, 실행, 결과 조회할 수 있으며, **테스트 코드 수정 후 애플리케이션 재시작 없이** 즉시 반영됩니다.

---

## 핵심 기능

| 기능 | 설명 |
|------|------|
| 테스트 발견 | JUnit Platform Launcher API로 테스트 코드 스캔 후 트리로 표시 |
| 테스트 실행 | 선택한 테스트 클래스를 별도 JVM에서 비동기 실행 |
| Hot Reload | 테스트 코드 수정해도 앱 재시작 불필요 |
| 결과 조회 | 성공/실패/에러/stdout을 트리 구조로 확인 |
| 소스 코드 보기 | JavaParser로 테스트 메서드 소스 코드 추출 |
| 실행 이력 | 모든 실행 결과 DB 저장 및 조회 |


---

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Backend | Spring Boot | 3.5.7 |
| Language | Java | 17 |
| Test Engine | JUnit Platform Launcher API | 5.x |
| Test Framework | JUnit Jupiter | 5.x |
| Database | DB2 + HikariCP | 11.5.x |
| Template | Thymeleaf | - |
| Build | Gradle (Kotlin DSL) | 8.x |
| Code Parsing | JavaParser | 3.26.3 |

---

## 프로젝트 구조

```
autotest/
├── src/main/java/testauto/
│   ├── TestAutoApplication.java        # Spring Boot 진입점 (@EnableAsync)
│   ├── TestCatalogInitializer.java     # 시작 시 테스트 카탈로그 초기화
│   │
│   ├── controller/                     # REST API & 웹 컨트롤러
│   │   ├── TestApiController.java      # REST API 엔드포인트
│   │   ├── TestPlatformMainController.java
│   │   └── TestResultController.java
│   │
│   ├── service/                        # 비즈니스 로직
│   │   ├── TestCatalogService.java     # 테스트 발견 및 카탈로그
│   │   ├── TestExecutionService.java   # 테스트 실행 (비동기)
│   │   ├── ProcessExecutorService.java # 별도 JVM 프로세스 실행
│   │   ├── TestTreeService.java        # 트리 구조 생성
│   │   └── SourceCodeService.java      # 소스 코드 추출
│   │
│   ├── repository/                     # 데이터 접근 계층
│   │   ├── TestNodeRepository.java
│   │   ├── TestNodeDbRepository.java
│   │   ├── TestNodeMemoryRepository.java
│   │   ├── TestExecutionRepository.java
│   │   └── TestExecutionDbRepository.java
│   │
│   ├── domain/                         # 도메인 모델
│   │   ├── TestNode.java               # 발견된 테스트 노드
│   │   ├── TestExecution.java          # 실행 이력
│   │   ├── TestResult.java             # 결과 (트리 구조)
│   │   ├── TestResultRecord.java       # 결과 레코드 (DB 매핑)
│   │   ├── TestSummary.java            # 결과 요약
│   │   └── TestStatus.java             # 상태 Enum
│   │
│   ├── dto/                            # 데이터 전송 객체
│   │   ├── TreeNodeDto.java
│   │   ├── ClassDetailDto.java
│   │   ├── TestMethodDto.java
│   │   ├── TestExecutionRequest.java
│   │   └── TestExecutionResponse.java
│   │
│   ├── exception/                      # 예외 처리
│   │   ├── GlobalExceptionHandler.java # 전역 예외 핸들러
│   │   └── ErrorResponse.java          # 에러 응답 DTO
│   │
│   ├── runner/                         # 별도 JVM 테스트 실행기
│   │   ├── TestRunner.java             # 별도 JVM 메인 클래스
│   │   └── TestRunnerListener.java     # TestExecutionListener 구현
│   │
│   └── util/junit/
│       └── WebTestListener.java        # 웹앱용 TestExecutionListener
│
├── sql/                                # DDL 스크립트 (DB2)
│   ├── c_test_node_catalog.ddl
│   ├── c_test_execution.ddl
│   ├── c_test_result.ddl
│   └── c_morning_monitor_manager.ddl   # 권한 관리 테이블
│
└── docs/                               # 상세 문서
    ├── ARCHITECTURE.md                 # 시스템 아키텍처
    ├── JUNIT_PLATFORM_GUIDE.md         # JUnit Platform API 가이드
    ├── HOT_RELOAD_MECHANISM.md         # Hot Reload 구현 원리
    └── API_REFERENCE.md                # REST API 레퍼런스
```

---

## 핵심 아키텍처

### 전체 시스템 흐름

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Web Browser                                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ TestApiController│  │ TestCatalogService│  │TestExecutionService│         │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘          │
│           │                     │                      │                     │
│           └─────────────────────┼──────────────────────┘                     │
│                                 ▼                                            │
│                    ┌────────────────────────┐                                │
│                    │ ProcessExecutorService │                                │
│                    └────────────┬───────────┘                                │
└─────────────────────────────────┼───────────────────────────────────────────┘
                                  │ Process 실행
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Separate JVM Process                                 │
│  ┌──────────────────────────────────────────────────────────────┐           │
│  │ TestRunner                                                    │           │
│  │  ├── LauncherFactory.create()                                │           │
│  │  ├── TestRunnerListener (TestExecutionListener)              │           │
│  │  └── JSON 결과 출력                                           │           │
│  └──────────────────────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               DB2 Database                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │C_TEST_NODE_CATALOG│  │ C_TEST_EXECUTION │  │  C_TEST_RESULT   │          │
│  │ (발견된 테스트)    │  │  (실행 이력)     │  │  (상세 결과)     │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                              │
│  ┌───────────────────────────────────┐                                      │
│  │  C_MORNING_MONITOR_MANAGER        │                                      │
│  │  (권한 관리)                       │                                      │
│  └───────────────────────────────────┘                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 별도 JVM 실행 전략 (Hot Reload의 핵심)

```
┌─────────────────────────────────────────────────────────────────┐
│  왜 별도 JVM인가?                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  문제: Spring Boot 앱이 실행 중일 때 테스트 코드를 수정하면     │
│        이미 로드된 클래스가 JVM에 캐시되어 있어 반영 안 됨       │
│                                                                 │
│  해결: 테스트 실행할 때마다 새 JVM 프로세스를 시작하면           │
│        항상 최신 컴파일된 클래스를 로드함                        │
│                                                                 │
│  ┌─────────────────┐      ┌─────────────────┐                  │
│  │  Spring Boot    │      │  새 JVM 프로세스 │                  │
│  │   (계속 실행)   │ ──▶  │   (매번 새로)    │                  │
│  │                 │      │                  │                  │
│  │  ProcessBuilder │      │  TestRunner.main │                  │
│  │  .start()       │      │  최신 클래스 로드│                  │
│  └─────────────────┘      └─────────────────┘                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 상세 문서

| 문서 | 설명 |
|------|------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 전체 시스템 아키텍처, 클래스 의존성, 데이터 흐름 |
| [docs/JUNIT_PLATFORM_GUIDE.md](docs/JUNIT_PLATFORM_GUIDE.md) | JUnit Platform Launcher API 활용 가이드 |
| [docs/HOT_RELOAD_MECHANISM.md](docs/HOT_RELOAD_MECHANISM.md) | Hot Reload 구현 원리 상세 |
| [docs/API_REFERENCE.md](docs/API_REFERENCE.md) | REST API 엔드포인트 레퍼런스 |

---

## REST API 요약

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/tests/tree` | 테스트 트리 조회 |
| POST | `/api/tests/refresh` | 카탈로그 새로고침 (컴파일 + 발견 + 트리 반환) |
| GET | `/api/tests/class/{className}` | 클래스 상세 정보 |
| POST | `/api/tests/run` | 테스트 실행 (비동기) |
| GET | `/api/tests/method/code` | 메서드 소스 코드 |
| GET | `/api/tests/server-time` | 서버 현재 시간 |
| GET | `/api/tests/dashboard` | 대시보드 통계 (오늘 실행, 주간 트렌드, 최근 실패 등) |
| GET | `/api/tests/check-auth` | 실행 권한 확인 |
| GET | `/api/tests/executions` | 실행 이력 목록 (기본 20개) |
| GET | `/api/tests/executions/{id}` | 특정 실행 조회 |
| GET | `/api/tests/executions/{id}/results` | 실행 결과 조회 (요약 + 트리) |

---

## 설정

### application.yml 주요 설정

```yaml
server:
  port: 9898

testcode:
  project-path: ${TESTCODE_PROJECT_PATH:/path/to/testcodes}
  root-package: ${TESTCODE_ROOT_PACKAGE:testauto.testcode}

spring:
  datasource:
    url: jdbc:db2://localhost:50000/bng000a
    username: db2inst1
    password: password
    driver-class-name: com.ibm.db2.jcc.DB2Driver
    hikari:
      maximum-pool-size: 100
      minimum-idle: 5
```

### 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `TESTCODE_PROJECT_PATH` | 테스트 코드 프로젝트 경로 | `/path/to/testcodes` |
| `TESTCODE_ROOT_PACKAGE` | 테스트 루트 패키지 | `testauto.testcode` |

---

## 워크플로우

```
[테스트 코드 수정]
       │
       ▼
[새로고침 버튼 클릭]  ──────────────────────────────────────┐
       │ POST /api/tests/refresh                           │
       │                                                   │
       ▼                                                   │
┌──────────────────────────────────────┐                   │
│ 1. gradle compileJava (테스트 컴파일) │                   │
│ 2. 별도 JVM에서 테스트 발견           │                   │
│    - LauncherFactory.create()        │                   │
│    - launcher.discover(request)      │                   │
│ 3. 결과를 DB에 저장                   │                   │
└──────────────────────────────────────┘                   │
       │                                                   │
       ▼                                                   │
[테스트 선택 → 실행 버튼 클릭]                              │
       │ POST /api/tests/run                               │
       │                                                   │
       ▼                                                   │
┌──────────────────────────────────────┐                   │
│ 1. executionId 생성 (UUID)           │                   │
│ 2. 실행 상태 DB 저장 (RUNNING)        │                   │
│ 3. 비동기로 별도 JVM 실행             │                   │
│    - TestRunner.main("run", classes) │                   │
│    - launcher.execute(request)       │                   │
│ 4. 결과 JSON 파싱                     │                   │
│ 5. 결과 DB 저장                       │                   │
└──────────────────────────────────────┘                   │
       │                                                   │
       ▼                                                   │
[결과 조회]                                                 │
       │ GET /api/tests/executions/{id}/results            │
       ▼                                                   │
[성공/실패/stdout/에러 확인] ◀─────────────────────────────┘
```

---

## 화면 구성

```
┌─────────────────────────────────────────────────────────────────┐
│  Header (로고, 타이틀)                                           │
├─────────────┬───────────────────────────────────────────────────┤
│             │                                                   │
│  Sidebar    │  Main Content                                     │
│             │                                                   │
│  [새로고침]  │  [Home] [Test Info] [Test Results]               │
│  [실행]     │                                                   │
│  [검색창]   │  - Home: 최근 실행 이력                            │
│             │  - Test Info: 클래스/메서드 정보, 소스코드         │
│  테스트     │  - Test Results: 실행 결과 트리                    │
│  트리       │                                                   │
│  ☑ Package  │                                                   │
│    ☑ Class1 │                                                   │
│    ☐ Class2 │                                                   │
│             │                                                   │
└─────────────┴───────────────────────────────────────────────────┘
```

