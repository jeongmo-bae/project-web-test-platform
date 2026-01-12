# 시스템 아키텍처

이 문서는 Web Test Platform의 전체 시스템 아키텍처를 설명합니다.

---

## 1. 전체 시스템 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 Client Layer                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Web Browser                                  │   │
│  │   Thymeleaf 템플릿 + Vanilla JavaScript                             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼ HTTP
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Application Layer                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       Spring Boot Application                        │   │
│  │                                                                      │   │
│  │   ┌────────────────┐ ┌────────────────┐ ┌────────────────┐         │   │
│  │   │   Controller   │ │    Service     │ │   Repository   │         │   │
│  │   │     Layer      │ │     Layer      │ │     Layer      │         │   │
│  │   └────────────────┘ └────────────────┘ └────────────────┘         │   │
│  │                              │                                       │   │
│  │                              ▼                                       │   │
│  │                 ┌────────────────────────┐                          │   │
│  │                 │  ProcessExecutorService │                          │   │
│  │                 └────────────┬───────────┘                          │   │
│  └──────────────────────────────┼──────────────────────────────────────┘   │
└─────────────────────────────────┼───────────────────────────────────────────┘
                                  │ Process 실행
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Execution Layer                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Separate JVM Process                            │   │
│  │                                                                      │   │
│  │   ┌────────────────┐ ┌────────────────┐ ┌────────────────┐         │   │
│  │   │   TestRunner   │ │   Launcher     │ │TestRunnerListener│        │   │
│  │   │    (main)      │ │   (JUnit)      │ │  (Listener)     │        │   │
│  │   └────────────────┘ └────────────────┘ └────────────────┘         │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               Data Layer                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         MySQL Database                               │   │
│  │                                                                      │   │
│  │   ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐      │   │
│  │   │C_TEST_NODE_CATALOG│ │C_TEST_EXECUTION│ │  C_TEST_RESULT  │      │   │
│  │   └─────────────────┘ └─────────────────┘ └─────────────────┘      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 레이어별 상세 설명

### 2.1 Controller Layer

| 클래스 | 역할 | 주요 엔드포인트 |
|--------|------|-----------------|
| `TestApiController` | REST API 제공 | `/api/tests/**` |
| `TestPlatformMainController` | 웹 페이지 렌더링 | `/`, `/test/**` |
| `TestResultController` | 결과 페이지 렌더링 | `/result/**` |

```java
@RestController
@RequestMapping("/api/tests")
public class TestApiController {

    @GetMapping("/tree")
    public TreeNodeDto getTestTree() { ... }

    @PostMapping("/refresh")
    public void refreshCatalog() { ... }

    @PostMapping("/run")
    public TestExecutionResponse runTests(@RequestBody TestExecutionRequest request) { ... }
}
```

### 2.2 Service Layer

| 클래스 | 책임 | 의존성 |
|--------|------|--------|
| `TestCatalogServiceImpl` | 테스트 발견 및 카탈로그 관리 | ProcessExecutorService, TestNodeRepository |
| `TestExecutionServiceImpl` | 테스트 실행 (비동기) | ProcessExecutorService, TestExecutionRepository |
| `ProcessExecutorService` | 별도 JVM 프로세스 실행 | - |
| `TestTreeServiceImpl` | 트리 구조 변환 | TestNodeRepository |
| `SourceCodeService` | 소스 코드 추출 | JavaParser |

### 2.3 Repository Layer

| 클래스 | 테이블 | 주요 기능 |
|--------|--------|----------|
| `TestNodeDbRepository` | C_TEST_NODE_CATALOG | 테스트 노드 CRUD |
| `TestExecutionDbRepository` | C_TEST_EXECUTION, C_TEST_RESULT | 실행 이력 및 결과 CRUD |

---

## 3. 클래스 의존성 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                Controllers                                   │
│                                                                              │
│  TestApiController ───┬─────────────────────────────────────────────────────┐
│                       │                                                      │
│                       ▼                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                          Services                                    │   │
│  │                                                                      │   │
│  │  ┌──────────────────┐    ┌──────────────────┐                       │   │
│  │  │TestCatalogService │────│ProcessExecutorSvc│────────────────┐      │   │
│  │  └────────┬─────────┘    └──────────────────┘                │      │   │
│  │           │                       ▲                          │      │   │
│  │           │                       │                          │      │   │
│  │  ┌────────▼─────────┐    ┌──────────────────┐                │      │   │
│  │  │TestExecutionSvc  │────┤ProcessExecutorSvc│                │      │   │
│  │  └────────┬─────────┘    └──────────────────┘                │      │   │
│  │           │                                                   │      │   │
│  │  ┌────────▼─────────┐                                        │      │   │
│  │  │  TestTreeService │                                        │      │   │
│  │  └────────┬─────────┘                                        │      │   │
│  │           │                                                   │      │   │
│  │  ┌────────▼─────────┐                                        │      │   │
│  │  │SourceCodeService │                                        │      │   │
│  │  └──────────────────┘                                        │      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                       │                                          │          │
│                       ▼                                          ▼          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Repositories                             │   │
│  │                                                                      │   │
│  │  ┌──────────────────┐         ┌──────────────────┐                  │   │
│  │  │TestNodeRepository│         │TestExecutionRepo │                  │   │
│  │  └──────────────────┘         └──────────────────┘                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                       │                         │                           │
│                       ▼                         ▼                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                          JdbcTemplate                                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 데이터 흐름

### 4.1 테스트 발견 (Discovery) 흐름

```
Client                  Spring Boot App              Separate JVM              DB
  │                          │                            │                     │
  │ POST /api/tests/refresh  │                            │                     │
  │─────────────────────────▶│                            │                     │
  │                          │                            │                     │
  │                          │ gradle compileJava         │                     │
  │                          │───────────────────────────▶│                     │
  │                          │                            │                     │
  │                          │ java TestRunner discover   │                     │
  │                          │───────────────────────────▶│                     │
  │                          │                            │                     │
  │                          │                            │ LauncherFactory     │
  │                          │                            │ .create()           │
  │                          │                            │                     │
  │                          │                            │ launcher.discover() │
  │                          │                            │                     │
  │                          │      JSON 결과              │                     │
  │                          │◀───────────────────────────│                     │
  │                          │                            │                     │
  │                          │ TestNode 변환 + 저장                             │
  │                          │──────────────────────────────────────────────────▶
  │                          │                                                  │
  │          200 OK          │                                                  │
  │◀─────────────────────────│                                                  │
```

### 4.2 테스트 실행 (Execution) 흐름

```
Client                  Spring Boot App              Separate JVM              DB
  │                          │                            │                     │
  │ POST /api/tests/run      │                            │                     │
  │─────────────────────────▶│                            │                     │
  │                          │                            │                     │
  │                          │ UUID 생성                                        │
  │                          │──────────────────────────────────────────────────▶
  │                          │ executionId 저장 (RUNNING)                        │
  │                          │                                                  │
  │     {executionId}        │                            │                     │
  │◀─────────────────────────│                            │                     │
  │                          │                            │                     │
  │                          │ @Async                     │                     │
  │                          │ ┌──────────────────────────────────────────┐    │
  │                          │ │                                          │    │
  │                          │ │ gradle compileJava       │              │    │
  │                          │ │────────────────────────▶│              │    │
  │                          │ │                          │              │    │
  │                          │ │ java TestRunner run      │              │    │
  │                          │ │────────────────────────▶│              │    │
  │                          │ │                          │              │    │
  │                          │ │                          │ launcher    │    │
  │                          │ │                          │ .execute()  │    │
  │                          │ │                          │              │    │
  │                          │ │        JSON 결과         │              │    │
  │                          │ │◀────────────────────────│              │    │
  │                          │ │                          │              │    │
  │                          │ │ 결과 파싱 + DB 저장                          │
  │                          │ │─────────────────────────────────────────────▶│
  │                          │ │ status = COMPLETED                           │
  │                          │ └──────────────────────────────────────────┘    │
  │                          │                            │                     │
  │ GET executions/{id}/results                           │                     │
  │─────────────────────────▶│                            │                     │
  │                          │                            │                     │
  │                          │◀──────────────────────────────────────────────────
  │      결과 트리 JSON       │                            │                     │
  │◀─────────────────────────│                            │                     │
```

---

## 5. 도메인 모델

### 5.1 핵심 도메인 객체

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Domain Models                                   │
│                                                                              │
│  ┌─────────────────────┐   ┌─────────────────────┐                         │
│  │      TestNode       │   │   TestExecution     │                         │
│  ├─────────────────────┤   ├─────────────────────┤                         │
│  │ uniqueId            │   │ executionId         │                         │
│  │ parentUniqueId      │   │ startedAt           │                         │
│  │ displayName         │   │ finishedAt          │                         │
│  │ className           │   │ totalTests          │                         │
│  │ type (CONTAINER/TEST)│   │ successCount        │                         │
│  └─────────────────────┘   │ failedCount         │                         │
│          │                 │ skippedCount        │                         │
│          │                 │ status              │                         │
│          ▼                 └─────────────────────┘                         │
│  C_TEST_NODE_CATALOG                │                                       │
│                                      │ 1:N                                   │
│                                      ▼                                       │
│                          ┌─────────────────────┐                            │
│                          │ TestResultRecord    │                            │
│                          ├─────────────────────┤                            │
│                          │ id                  │                            │
│                          │ executionId (FK)    │                            │
│                          │ testId              │                            │
│                          │ parentTestId        │                            │
│                          │ displayName         │                            │
│                          │ status              │                            │
│                          │ durationMillis      │                            │
│                          │ errorMessage        │                            │
│                          │ stackTrace          │                            │
│                          │ stdout              │                            │
│                          └─────────────────────┘                            │
│                                      │                                       │
│                                      ▼                                       │
│                              C_TEST_RESULT                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 DTO 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DTO Objects                                     │
│                                                                              │
│  ┌─────────────────────┐   ┌─────────────────────┐                         │
│  │     TreeNodeDto     │   │TestExecutionRequest │                         │
│  ├─────────────────────┤   ├─────────────────────┤                         │
│  │ name                │   │ classNames          │                         │
│  │ uniqueId            │   └─────────────────────┘                         │
│  │ className           │                                                    │
│  │ type (PACKAGE/CLASS)│   ┌─────────────────────┐                         │
│  │ children[]          │   │TestExecutionResponse│                         │
│  └─────────────────────┘   ├─────────────────────┤                         │
│                            │ executionId         │                         │
│  ┌─────────────────────┐   │ status              │                         │
│  │   ClassDetailDto    │   │ message             │                         │
│  ├─────────────────────┤   └─────────────────────┘                         │
│  │ className           │                                                    │
│  │ fullClassName       │   ┌─────────────────────┐                         │
│  │ methods[]           │   │   TestMethodDto     │                         │
│  └─────────────────────┘   ├─────────────────────┤                         │
│                            │ methodName          │                         │
│                            │ displayName         │                         │
│                            │ uniqueId            │                         │
│                            │ isNestedClass       │                         │
│                            │ children[]          │                         │
│                            └─────────────────────┘                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. 데이터베이스 스키마

### 6.1 ERD

```
┌─────────────────────────┐
│  C_TEST_NODE_CATALOG    │
├─────────────────────────┤
│ PK unique_id VARCHAR    │
│    parent_unique_id     │
│    displayname          │
│    classname            │
│    type                 │
│    updatedat            │
└─────────────────────────┘


┌─────────────────────────┐       ┌─────────────────────────┐
│   C_TEST_EXECUTION      │       │     C_TEST_RESULT       │
├─────────────────────────┤       ├─────────────────────────┤
│ PK execution_id VARCHAR │◀──┐   │ PK id BIGINT            │
│    started_at           │   │   │ FK execution_id ────────┼───┘
│    finished_at          │   │   │    test_id              │
│    total_tests          │   │   │    parent_test_id       │
│    success_count        │   │   │    display_name         │
│    failed_count         │   │   │    status               │
│    skipped_count        │   │   │    duration_millis      │
│    total_duration_millis│   │   │    error_message        │
│    requester_ip         │   │   │    stack_trace          │
│    class_names          │   │   │    stdout               │
│    status               │   └───│                         │
└─────────────────────────┘       └─────────────────────────┘
```

### 6.2 테이블 설명

| 테이블 | 목적 | 주요 컬럼 |
|--------|------|----------|
| C_TEST_NODE_CATALOG | 발견된 테스트 노드 저장 | unique_id (JUnit UniqueId), type (CONTAINER/TEST) |
| C_TEST_EXECUTION | 테스트 실행 이력 | execution_id (UUID), status (RUNNING/COMPLETED/FAILED) |
| C_TEST_RESULT | 개별 테스트 결과 | test_id, parent_test_id (트리 구조), status, stdout |

---

## 7. 핵심 설계 패턴

### 7.1 인터페이스 기반 설계

```java
// 인터페이스 정의
public interface TestCatalogService {
    void refreshTestCatalog();
    ClassDetailDto getClassDetail(String className);
}

public interface TestNodeRepository {
    void save(TestNode node);
    List<TestNode> findAll();
    List<TestNode> findByParentId(String parentId);
}

// 구현 클래스
@Service
public class TestCatalogServiceImpl implements TestCatalogService { ... }

@Repository
public class TestNodeDbRepository implements TestNodeRepository { ... }

// 테스트용 메모리 구현
public class TestNodeMemoryRepository implements TestNodeRepository { ... }
```

### 7.2 비동기 처리 (@Async)

```java
@EnableAsync  // 애플리케이션 클래스에 선언
public class TestAutoApplication { ... }

@Service
public class TestExecutionServiceImpl {

    @Async  // 별도 스레드에서 실행
    public void executeTestsAsync(String executionId, List<String> classNames) {
        // 시간이 걸리는 테스트 실행
        processExecutorService.runTests(classNames);
    }
}
```

### 7.3 스레드 안전성

```java
public class TestResult {
    // 동시 접근 가능한 컬렉션 사용
    private List<TestResult> children = new CopyOnWriteArrayList<>();
}

public class TestRunnerListener {
    // 동시 접근을 위한 ConcurrentHashMap
    private final Map<String, TestResultDto> nodeMap = new ConcurrentHashMap<>();
}
```

---

## 8. 확장 포인트

### 8.1 다른 테스트 엔진 지원

현재는 JUnit Jupiter만 지원하지만, JUnit Platform의 다른 엔진도 지원 가능:

```java
// TestNG 엔진 추가 예시
dependencies {
    implementation("org.junit.support:testng-engine:1.0.4")
}
```

### 8.2 결과 저장소 변경

Repository 인터페이스를 구현하여 다른 저장소 사용 가능:

```java
// Redis 구현 예시
@Repository
public class TestNodeRedisRepository implements TestNodeRepository {
    private final RedisTemplate<String, TestNode> redisTemplate;
    // ...
}
```

### 8.3 알림 기능 추가

테스트 완료 시 알림 발송:

```java
@Async
public void executeTestsAsync(String executionId, List<String> classNames) {
    RunResult result = processExecutorService.runTests(classNames);
    saveResults(executionId, result);

    // 알림 발송 추가
    notificationService.sendTestCompleteNotification(executionId, result);
}
```

---

## 9. 성능 고려사항

### 9.1 연결 풀 설정

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 100    # 최대 동시 연결
      minimum-idle: 5           # 최소 유휴 연결
      connection-timeout: 30000 # 연결 타임아웃 (30초)
```

### 9.2 배치 처리

```java
// 대량 INSERT 시 배치 사용
public void saveAllResults(List<TestResultRecord> records) {
    String sql = "INSERT INTO c_test_result (...) VALUES (?, ?, ?, ...)";

    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) { ... }

        @Override
        public int getBatchSize() { return records.size(); }
    });
}
```

### 9.3 프로세스 타임아웃

```java
// 테스트 실행 타임아웃 설정
Process process = pb.start();
boolean finished = process.waitFor(10, TimeUnit.MINUTES);
if (!finished) {
    process.destroyForcibly();
    throw new TimeoutException("Test execution timed out");
}
```
