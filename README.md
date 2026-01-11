# Web Test Platform

JUnit 5 기반 테스트 자동화 플랫폼. 웹 UI에서 테스트를 발견, 실행, 결과 조회 가능.

---

## 목차

**온보딩**
1. [프로젝트 소개](#프로젝트-소개)
2. [화면 구성](#화면-구성)
3. [트러블슈팅](#트러블슈팅)

**기술 문서**
4. [아키텍처](#아키텍처)
5. [프로젝트 구조](#프로젝트-구조)
6. [API 명세](#api-명세)
7. [클래스 상세](#클래스-상세)
8. [데이터베이스](#데이터베이스)
9. [설정](#설정)
10. [실행 방법](#실행-방법)

---

# 온보딩

---

## 프로젝트 소개

### What

**웹 기반 테스트 실행 플랫폼**. 브라우저에서 JUnit 테스트를 선택하고 실행하고 결과를 확인할 수 있음.

### Why
- **웹 브라우저만 있으면** 테스트 실행 가능
- **테스트 코드 수정해도 앱 재시작 필요 없음** (별도 JVM에서 실행)
- **누구나** 테스트 실행 가능 (QA, 기획자 등)
- **결과가 DB에 저장**되어 이력 관리 가능

### 주요 기능

| 기능 | 설명 |
|------|------|
| 테스트 발견 | 테스트 코드 스캔해서 트리로 표시 |
| 테스트 실행 | 선택한 테스트 클래스 실행 |
| 결과 조회 | 성공/실패/에러/stdout 확인 |
| 소스 코드 보기 | 테스트 메서드 소스 코드 확인 |
| 실행 이력 | 과거 실행 결과 조회 |

### 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.5.7, Java 17 |
| Frontend | Thymeleaf, Vanilla JS |
| Database | MySQL |
| Test Framework | JUnit 5 (Jupiter) |
| Build | Gradle (Kotlin DSL) |

---

## 화면 구성

### 사이드바 기능

| 요소 | 기능 |
|------|------|
| 새로고침 버튼 | 테스트 코드 컴파일 + 카탈로그 갱신 |
| 실행 버튼 | 체크된 클래스 테스트 실행 |
| 검색창 | 클래스명으로 필터링 |
| 트리 뷰 | 패키지 → 클래스 계층 구조 |
| 체크박스 | 실행할 클래스 선택 (클래스에만 있음) |

### Test Info 패널

클래스 클릭 시 표시:
- 클래스명
- 테스트 메서드 목록
- Nested 클래스 구조
- 각 메서드의 @DisplayName

### Test Results 패널

테스트 실행 후 표시:
- 요약: 전체/성공/실패/스킵 수, 총 소요시간
- 결과 트리: 클래스 → 메서드 계층
- 각 테스트별: 상태, 소요시간, stdout, 에러 메시지

---

## 트러블슈팅

### 자주 발생하는 문제

#### 1. 앱 시작 시 "Failed to refresh test catalog" 경고

```
WARN - Failed to refresh test catalog on startup: ...
```

**원인**: 테스트 코드 프로젝트 경로가 잘못되었거나 존재하지 않음

**해결**:
```bash
# 1. 테스트 코드 프로젝트 경로 확인
ls /opt/testcodes  # 또는 설정한 경로

# 2. 경로가 없으면 생성
cp -r testcode-template /opt/testcodes

# 3. 환경변수 확인
echo $TESTCODE_PROJECT_PATH
```

---

#### 2. "Gradle compile failed" 에러

**원인**: 테스트 코드 프로젝트에서 컴파일 에러 발생

**해결**:
```bash
# 테스트 코드 프로젝트로 이동해서 직접 컴파일 시도
cd /opt/testcodes
./gradlew compileJava

# 에러 메시지 확인 후 수정
```

---

#### 3. MySQL 연결 실패

```
Cannot create PoolableConnectionFactory
```

**원인**: MySQL이 실행 중이 아니거나, 접속 정보가 틀림

**해결**:
```bash
# MySQL 실행 확인
brew services list  # Mac
systemctl status mysql  # Linux

# MySQL 시작
brew services start mysql  # Mac
sudo systemctl start mysql  # Linux

# 접속 테스트
mysql -u root -p -h localhost

# 비밀번호 확인 후 application.yml 수정
```

---

#### 4. "Class not found" 에러 (테스트 실행 시)

**원인**: 테스트 코드가 컴파일되지 않았거나, 패키지명이 다름

**해결**:
```bash
# 1. 새로고침 버튼 클릭 (컴파일 + 발견)

# 2. 직접 컴파일 확인
cd /opt/testcodes
./gradlew compileJava
ls build/classes/java/main/testauto/testcode/

# 3. 패키지명 확인 (testauto.testcode로 시작해야 함)
```

---

#### 5. 테스트 코드 수정했는데 반영 안 됨

**원인**: 새로고침 안 함

**해결**:
1. 서버의 테스트 코드 프로젝트에서 `git pull`
2. 웹 UI에서 **새로고침 버튼 클릭**
3. 테스트 실행

```bash
# 서버에서
cd /opt/testcodes
git pull

# 그 다음 웹 UI에서 새로고침 클릭
```

---

#### 6. Port 9898 already in use

**원인**: 이미 앱이 실행 중이거나 다른 프로세스가 포트 사용 중

**해결**:
```bash
# 포트 사용 프로세스 확인
lsof -i :9898

# 프로세스 종료
kill -9 <PID>

# 또는 다른 포트로 실행
SERVER_PORT=9999 ./gradlew bootRun
```

---

#### 7. Lombok 관련 컴파일 에러 (IDE)

```
cannot find symbol: method builder()
```

**원인**: IDE에서 Lombok annotation processing이 안 됨

**해결 (IntelliJ)**:
1. Settings → Build → Compiler → Annotation Processors
2. "Enable annotation processing" 체크
3. Settings → Plugins → Lombok 설치 확인
4. IDE 재시작

---

#### 8. gradlew 실행 권한 에러

```
Permission denied: ./gradlew
```

**해결**:
```bash
chmod +x gradlew
./gradlew bootRun
```

---

### 로그 확인 방법

```bash
# 실행 중 로그 확인
./gradlew bootRun  # 콘솔에 출력됨

# 로그 레벨 변경 (더 상세히)
# application.yml에 추가:
logging:
  level:
    testauto: DEBUG
    org.springframework: INFO
```

---

# 기술 문서

## 아키텍처

### 전체 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Web Browser                                 │
│  ┌─────────────┐  ┌──────────────────┐  ┌─────────────────────────────┐ │
│  │   Sidebar   │  │  Test Info Panel │  │     Test Results Panel      │ │
│  │  (트리 뷰)   │  │   (클래스 상세)   │  │      (실행 결과 표시)        │ │
│  └─────────────┘  └──────────────────┘  └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application                          │
│                              (port 9898)                                 │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                         Controller Layer                          │   │
│  │   TestApiController    TestPlatformMainController                │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                          Service Layer                            │   │
│  │  TestCatalogService  TestExecutionService  TestTreeService       │   │
│  │  ProcessExecutorService  SourceCodeService                       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                        Repository Layer                           │   │
│  │      TestNodeRepository          TestExecutionRepository         │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
          │                                              │
          ▼                                              ▼
┌──────────────────┐                        ┌─────────────────────────────┐
│      MySQL       │                        │     별도 JVM (TestRunner)    │
│    (bng000a)     │                        │   - 테스트 발견 (discover)    │
│                  │                        │   - 테스트 실행 (run)         │
└──────────────────┘                        └─────────────────────────────┘
                                                         │
                                                         ▼
                                            ┌─────────────────────────────┐
                                            │   테스트 코드 프로젝트        │
                                            │   (/opt/testcodes)          │
                                            │   - 별도 Git 저장소          │
                                            │   - Gradle 빌드             │
                                            └─────────────────────────────┘
```

### 핵심 설계 포인트

#### 1. 별도 JVM 실행 방식
- 테스트 코드 변경 시 앱 재시작 없이 반영 가능
- `ProcessBuilder`로 별도 JVM 프로세스 spawn
- Gradle로 컴파일 → TestRunner로 실행 → JSON 결과 반환

```
[새로고침/실행 버튼 클릭]
         │
         ▼
[Gradle compileJava]  ← 테스트 코드 컴파일
         │
         ▼
[별도 JVM: TestRunner]  ← 테스트 발견/실행
         │
         ▼
[JSON 결과 stdout 출력]
         │
         ▼
[메인 앱에서 파싱 → DB 저장]
```

#### 2. 비동기 테스트 실행
- 테스트 실행 요청 시 `executionId` 즉시 반환
- `@Async`로 백그라운드 실행
- 프론트엔드에서 폴링으로 결과 조회

#### 3. 트리 구조 관리
- DB에는 평탄화된 부모-자식 ID로 저장
- 애플리케이션에서 트리 구조로 재구성
- 패키지 → 클래스 → Nested 클래스 → 메서드 계층

---

## 프로젝트 구조

```
project-web-test-platform/
├── src/main/java/testauto/
│   ├── TestAutoApplication.java          # Spring Boot 진입점
│   ├── TestCatalogInitializer.java       # 앱 시작 시 카탈로그 초기화
│   │
│   ├── controller/
│   │   ├── TestApiController.java        # REST API 엔드포인트
│   │   ├── TestPlatformMainController.java  # 웹 페이지 라우팅
│   │   └── TestResultController.java     # 결과 페이지 처리
│   │
│   ├── service/
│   │   ├── TestCatalogService.java       # 테스트 카탈로그 인터페이스
│   │   ├── TestCatalogServiceImpl.java   # 테스트 발견/카탈로그 관리
│   │   ├── TestExecutionService.java     # 테스트 실행 인터페이스
│   │   ├── TestExecutionServiceImpl.java # 테스트 실행/결과 관리
│   │   ├── TestTreeService.java          # 트리 구성 인터페이스
│   │   ├── TestTreeServiceImpl.java      # 사이드바 트리 구성
│   │   ├── ProcessExecutorService.java   # 외부 프로세스 실행
│   │   └── SourceCodeService.java        # 소스 코드 추출
│   │
│   ├── repository/
│   │   ├── TestNodeRepository.java       # 테스트 노드 저장소 인터페이스
│   │   ├── TestNodeDbRepository.java     # DB 구현
│   │   ├── TestExecutionRepository.java  # 실행 이력 저장소 인터페이스
│   │   └── TestExecutionDbRepository.java # DB 구현
│   │
│   ├── domain/
│   │   ├── TestNode.java                 # 테스트 노드 (카탈로그용)
│   │   ├── TestExecution.java            # 실행 이력
│   │   ├── TestResult.java               # 실행 결과 (트리)
│   │   ├── TestResultRecord.java         # 실행 결과 (DB 레코드)
│   │   ├── TestStatus.java               # 상태 enum
│   │   └── TestSummary.java              # 결과 요약
│   │
│   ├── dto/
│   │   ├── TestExecutionRequest.java     # 실행 요청 DTO
│   │   ├── TestExecutionResponse.java    # 실행 응답 DTO
│   │   ├── ClassDetailDto.java           # 클래스 상세 DTO
│   │   ├── TestMethodDto.java            # 메서드 정보 DTO
│   │   └── TreeNodeDto.java              # 트리 노드 DTO
│   │
│   ├── runner/
│   │   ├── TestRunner.java               # 별도 JVM용 테스트 러너
│   │   └── TestRunnerListener.java       # JUnit 실행 리스너
│   │
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java   # 전역 예외 처리
│   │   └── ErrorResponse.java            # 에러 응답 DTO
│   │
│   └── util/junit/
│       └── WebTestListener.java          # (레거시) JUnit 리스너
│
├── src/main/resources/
│   ├── application.yml                   # 애플리케이션 설정
│   ├── templates/
│   │   └── index.html                    # 메인 페이지 (Thymeleaf)
│   └── static/
│       ├── css/testauto.css              # 스타일시트
│       └── js/
│           ├── index.js                  # 메인 JavaScript
│           └── side-bar.js               # 사이드바 기능
│
├── sql/
│   ├── c_test_node_catalog.ddl           # 테스트 카탈로그 테이블
│   ├── c_test_execution.ddl              # 실행 이력 테이블
│   └── c_test_result.ddl                 # 실행 결과 테이블
│
├── testcode-template/                    # 테스트 코드 프로젝트 템플릿
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/main/java/testauto/testcode/
│       ├── e2e/
│       └── unit/
│
└── build.gradle.kts                      # 메인 빌드 설정
```

---

## API 명세

### 기본 경로: `/api/tests`

### 1. 테스트 트리 조회

**GET** `/api/tests/tree`

테스트 카탈로그를 트리 형태로 반환.

**Response:**
```json
{
  "name": "testcode",
  "uniqueId": null,
  "className": null,
  "type": "PACKAGE",
  "children": [
    {
      "name": "e2e",
      "type": "PACKAGE",
      "children": [
        {
          "name": "SampleTest",
          "uniqueId": "[engine:junit-jupiter]/[class:testauto.testcode.e2e.SampleTest]",
          "className": "testauto.testcode.e2e.SampleTest",
          "type": "CLASS",
          "children": []
        }
      ]
    }
  ]
}
```

---

### 2. 테스트 카탈로그 새로고침

**POST** `/api/tests/refresh`

테스트 코드를 컴파일하고 카탈로그 갱신.

**Response:**
```json
{
  "status": "SUCCESS",
  "message": "Test catalog refreshed",
  "tree": { ... }  // 갱신된 트리
}
```

---

### 3. 클래스 상세 조회

**GET** `/api/tests/class/{className}`

특정 테스트 클래스의 메서드 목록 조회.

**Example:**
```
GET /api/tests/class/testauto.testcode.unit.SampleTest
```

**Response:**
```json
{
  "className": "SampleTest",
  "fullClassName": "testauto.testcode.unit.SampleTest",
  "methods": [
    {
      "methodName": "successTest",
      "displayName": "성공 테스트 입니다",
      "uniqueId": "[engine:junit-jupiter]/[class:...]/[method:successTest()]",
      "isNestedClass": false,
      "children": []
    },
    {
      "methodName": null,
      "displayName": "Nested Group 테스트 입니다",
      "uniqueId": "[engine:junit-jupiter]/[class:...]/[nested-class:NestedGroup]",
      "isNestedClass": true,
      "children": [
        {
          "methodName": "nestedTest",
          "displayName": "Nested1 테스트 입니다",
          "isNestedClass": false
        }
      ]
    }
  ]
}
```

---

### 4. 테스트 실행

**POST** `/api/tests/run`

선택한 테스트 클래스 실행. 비동기로 처리되며 `executionId` 즉시 반환.

**Request:**
```json
{
  "classNames": [
    "testauto.testcode.unit.SampleTest",
    "testauto.testcode.e2e.EbmTest"
  ]
}
```

**Response:**
```json
{
  "executionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "message": "Test execution started"
}
```

---

### 5. 메서드 소스 코드 조회

**GET** `/api/tests/method/{className}/{methodName}/code`

특정 테스트 메서드의 소스 코드 조회.

**Example:**
```
GET /api/tests/method/testauto.testcode.unit.SampleTest/successTest/code
```

**Response:**
```json
{
  "code": "@Test\n@DisplayName(\"성공 테스트 입니다\")\nvoid successTest() {\n    System.out.println(\"successTest run\");\n    Assertions.assertTrue(true);\n}"
}
```

---

### 6. 실행 이력 조회

**GET** `/api/tests/executions?limit=20`

최근 테스트 실행 이력 조회.

**Response:**
```json
[
  {
    "executionId": "550e8400-e29b-41d4-a716-446655440000",
    "startedAt": "2026-01-11T10:30:00",
    "finishedAt": "2026-01-11T10:30:05",
    "totalTests": 10,
    "successCount": 8,
    "failedCount": 2,
    "skippedCount": 0,
    "totalDurationMillis": 5000,
    "requesterIp": "192.168.1.100",
    "classNames": "testauto.testcode.unit.SampleTest",
    "status": "COMPLETED"
  }
]
```

---

### 7. 특정 실행 조회

**GET** `/api/tests/executions/{executionId}`

**Response:**
```json
{
  "executionId": "550e8400-...",
  "startedAt": "2026-01-11T10:30:00",
  "finishedAt": "2026-01-11T10:30:05",
  "totalTests": 10,
  "successCount": 8,
  "failedCount": 2,
  "skippedCount": 0,
  "status": "COMPLETED"
}
```

---

### 8. 실행 결과 상세 조회

**GET** `/api/tests/executions/{executionId}/results`

테스트 결과를 트리 형태 + 요약 정보로 반환.

**Response:**
```json
{
  "summary": {
    "total": 4,
    "success": 2,
    "failed": 2,
    "skipped": 0,
    "totalDurationMillis": 150
  },
  "results": [
    {
      "id": "[engine:junit-jupiter]/[class:...SampleTest]",
      "displayName": "SampleTest",
      "status": "FAILED",
      "durationMillis": 150,
      "errorMessage": null,
      "stackTrace": null,
      "stdout": null,
      "children": [
        {
          "id": "[engine:junit-jupiter]/[class:...]/[method:successTest()]",
          "displayName": "성공 테스트 입니다",
          "status": "SUCCESS",
          "durationMillis": 50,
          "stdout": "[beforeEach]\nsuccessTest run\n",
          "children": []
        },
        {
          "id": "[engine:junit-jupiter]/[class:...]/[method:failedTest()]",
          "displayName": "실패 테스트 입니다",
          "status": "FAILED",
          "durationMillis": 30,
          "errorMessage": "의도적으로 실패",
          "stackTrace": "org.opentest4j.AssertionFailedError: 의도적으로 실패\n\tat ...",
          "stdout": "[beforeEach]\nfailedTest run\n",
          "children": []
        }
      ]
    }
  ]
}
```

---

### 9. 서버 시간 조회

**GET** `/api/tests/server-time`

**Response:**
```json
{
  "today": "2026-01-11"
}
```

---

## 클래스 상세

### Controller

#### TestApiController

REST API 엔드포인트 제공. 테스트 발견, 실행, 결과 조회 처리.

```java
@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestApiController {

    private final TestTreeService testTreeService;
    private final TestCatalogService testCatalogService;
    private final TestExecutionService testExecutionService;
    private final SourceCodeService sourceCodeService;

    // 테스트 트리 조회
    @GetMapping("/tree")
    public ResponseEntity<TreeNodeDto> getTestTree() {
        TreeNodeDto tree = testTreeService.buildTree();
        return ResponseEntity.ok(tree);
    }

    // 테스트 실행 (비동기)
    @PostMapping("/run")
    public ResponseEntity<TestExecutionResponse> runTests(
            @Valid @RequestBody TestExecutionRequest request,
            HttpServletRequest httpRequest) {
        String requesterIp = getClientIp(httpRequest);
        String executionId = testExecutionService.submitTests(
            request.getClassNames(), requesterIp);
        return ResponseEntity.ok(TestExecutionResponse.builder()
                .executionId(executionId)
                .status("RUNNING")
                .message("Test execution started")
                .build());
    }

    // ... 기타 메서드
}
```

---

### Service

#### TestCatalogServiceImpl

테스트 카탈로그 관리. 별도 JVM에서 테스트 발견 후 DB 저장.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TestCatalogServiceImpl implements TestCatalogService {

    private final TestNodeRepository repository;
    private final ProcessExecutorService processExecutorService;

    @Value("${testcode.root-package:testauto.testcode}")
    private String testcodeRootPackage;

    @Override
    public void refreshTestCatalog() {
        try {
            // 1. Gradle로 테스트 코드 컴파일
            processExecutorService.compileTestCode();

            // 2. 별도 JVM에서 테스트 발견
            TestRunner.DiscoverResult result =
                processExecutorService.discoverTests(testcodeRootPackage);

            if (!result.success()) {
                throw new RuntimeException("Test discovery failed: " + result.error());
            }

            // 3. DB에 저장 (기존 데이터 삭제 후)
            repository.deleteAll();
            List<TestNode> testNodes = result.nodes().stream()
                    .map(dto -> TestNode.builder()
                            .uniqueId(dto.uniqueId())
                            .parentUniqueId(dto.parentUniqueId())
                            .displayName(dto.displayName())
                            .className(dto.className())
                            .type(dto.type())
                            .build())
                    .collect(Collectors.toList());
            repository.saveAll(testNodes);

            log.info("Test catalog refreshed: {} nodes discovered", testNodes.size());
        } catch (Exception e) {
            log.error("Failed to refresh test catalog", e);
            throw new RuntimeException("Failed to refresh test catalog", e);
        }
    }
}
```

---

#### TestExecutionServiceImpl

테스트 실행 관리. 비동기로 별도 JVM에서 실행 후 결과 저장.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TestExecutionServiceImpl implements TestExecutionService {

    private final TestExecutionRepository executionRepository;
    private final ProcessExecutorService processExecutorService;

    @Override
    public String submitTests(List<String> classNames, String requesterIp) {
        String executionId = UUID.randomUUID().toString();

        // 실행 시작 기록
        TestExecution execution = TestExecution.builder()
                .executionId(executionId)
                .startedAt(LocalDateTime.now())
                .requesterIp(requesterIp)
                .classNames(String.join(",", classNames))
                .status("RUNNING")
                .build();
        executionRepository.saveExecution(execution);

        // 비동기 실행 시작
        executeTestsAsync(executionId, classNames);

        return executionId;  // 즉시 반환
    }

    @Async
    public void executeTestsAsync(String executionId, List<String> classNames) {
        try {
            // 1. 컴파일
            processExecutorService.compileTestCode();

            // 2. 별도 JVM에서 테스트 실행
            TestRunner.RunResult runResult = processExecutorService.runTests(classNames);

            // 3. 결과 저장
            saveResultsToDb(executionId, runResult);

        } catch (Exception e) {
            log.error("Failed to execute tests", e);
            // 실패 상태로 업데이트
            updateExecutionStatus(executionId, "FAILED");
        }
    }
}
```

---

#### ProcessExecutorService

외부 프로세스 실행. Gradle 컴파일 및 TestRunner 실행.

```java
@Slf4j
@Service
public class ProcessExecutorService {

    private static final String RESULT_MARKER = "###TEST_RUNNER_RESULT###";

    @Value("${testcode.project-path}")
    private String testcodeProjectPath;

    // Gradle로 테스트 코드 컴파일
    public void compileTestCode() throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(testcodeProjectPath));
        pb.command("./gradlew", "compileJava", "--quiet");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);

        if (!finished || process.exitValue() != 0) {
            throw new RuntimeException("Gradle compile failed");
        }
    }

    // 테스트 발견 (별도 JVM)
    public TestRunner.DiscoverResult discoverTests(String rootPackage) throws Exception {
        List<String> command = buildJavaCommand("discover", rootPackage);
        String output = executeProcess(command);
        return parseResult(output, TestRunner.DiscoverResult.class);
    }

    // 테스트 실행 (별도 JVM)
    public TestRunner.RunResult runTests(List<String> classNames) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("run");
        args.addAll(classNames);

        List<String> command = buildJavaCommand(args.toArray(new String[0]));
        String output = executeProcess(command);
        return parseResult(output, TestRunner.RunResult.class);
    }

    // classpath 구성
    private String buildClasspath() throws Exception {
        List<String> paths = new ArrayList<>();

        // 1. 현재 애플리케이션의 classpath (TestRunner 포함)
        paths.add(System.getProperty("java.class.path"));

        // 2. 테스트 코드의 컴파일된 클래스
        Path testClassesPath = Path.of(testcodeProjectPath,
            "build", "classes", "java", "main");
        if (Files.exists(testClassesPath)) {
            paths.add(testClassesPath.toString());
        }

        return String.join(":", paths);
    }
}
```

---

#### TestRunner (별도 JVM용)

독립 실행 가능한 테스트 러너. discover/run 두 가지 모드 지원.

```java
public class TestRunner {

    private static final String RESULT_MARKER = "###TEST_RUNNER_RESULT###";

    public static void main(String[] args) {
        String mode = args[0];  // "discover" or "run"

        switch (mode) {
            case "discover" -> {
                String rootPackage = args[1];
                runDiscover(rootPackage);
            }
            case "run" -> {
                List<String> classNames = Arrays.asList(args).subList(1, args.length);
                runTests(classNames);
            }
        }
    }

    // 테스트 발견
    private static void runDiscover(String rootPackage) throws Exception {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage(rootPackage))
                .build();

        TestPlan testPlan = launcher.discover(request);
        List<TestNodeDto> nodes = collectNodes(testPlan);

        // JSON으로 출력
        printResult(new DiscoverResult(true, null, nodes));
    }

    // 테스트 실행
    private static void runTests(List<String> classNames) throws Exception {
        TestRunnerListener listener = new TestRunnerListener();
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);

        List<DiscoverySelector> selectors = classNames.stream()
                .map(DiscoverySelectors::selectClass)
                .toList();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors)
                .build();

        launcher.execute(request);

        // JSON으로 출력
        printResult(new RunResult(
            true, null,
            listener.buildSummary(),
            listener.findRootResults()
        ));
    }

    // 결과를 JSON으로 stdout 출력
    private static void printResult(Object result) throws Exception {
        String json = objectMapper.writeValueAsString(result);
        System.out.println(RESULT_MARKER);  // 마커로 결과 시작 표시
        System.out.println(json);
    }

    // 결과 DTO들
    public record DiscoverResult(boolean success, String error, List<TestNodeDto> nodes) {}
    public record RunResult(boolean success, String error,
                           TestSummaryDto summary, List<TestResultDto> results) {}
    public record TestNodeDto(String uniqueId, String parentUniqueId,
                             String displayName, String className, String type) {}
    public record TestResultDto(String id, String displayName, String status,
                               long durationMillis, String errorMessage,
                               String stackTrace, String stdout,
                               List<TestResultDto> children) {}
}
```

---

#### TestRunnerListener

JUnit 실행 리스너. 각 테스트의 상태, 시간, stdout, 에러 캡처.

```java
public class TestRunnerListener implements TestExecutionListener {

    private final Map<String, MutableTestResult> nodeMap = new ConcurrentHashMap<>();
    private final Map<String, ByteArrayOutputStream> stdoutCaptures = new ConcurrentHashMap<>();
    private final PrintStream originalOut = System.out;

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        String id = testIdentifier.getUniqueId();

        // 노드 생성
        MutableTestResult node = new MutableTestResult(id, testIdentifier.getDisplayName());
        nodeMap.put(id, node);

        // 부모-자식 관계 설정
        testIdentifier.getParentId().ifPresent(parentId -> {
            MutableTestResult parent = nodeMap.get(parentId);
            if (parent != null) parent.children.add(node);
        });

        // 테스트 메서드인 경우 stdout 캡처 시작
        if (testIdentifier.isTest()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stdoutCaptures.put(id, baos);
            System.setOut(new PrintStream(new TeeOutputStream(originalOut, baos)));
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
                                  TestExecutionResult result) {
        String id = testIdentifier.getUniqueId();
        MutableTestResult node = nodeMap.get(id);

        // 상태 설정
        node.status = switch (result.getStatus()) {
            case SUCCESSFUL -> "SUCCESS";
            case FAILED -> "FAILED";
            case ABORTED -> "SKIPPED";
        };

        // 에러 정보 저장
        result.getThrowable().ifPresent(t -> {
            node.errorMessage = t.getMessage();
            node.stackTrace = getStackTraceAsString(t);
        });

        // stdout 캡처 종료
        if (testIdentifier.isTest()) {
            System.setOut(originalOut);
            ByteArrayOutputStream baos = stdoutCaptures.remove(id);
            if (baos != null) node.stdout = baos.toString();
        }
    }
}
```

---

#### SourceCodeService

JavaParser로 소스 코드에서 메서드 추출.

```java
@Service
public class SourceCodeService {

    private static final List<Path> SEARCH_PATHS = List.of(
        Path.of("src/test/java"),
        Path.of("src/main/java")
    );

    public String getMethodSourceCode(String className, String methodName) {
        // 클래스명 → 파일 경로 변환
        String relativePath = className.replace('.', '/') + ".java";

        for (Path searchPath : SEARCH_PATHS) {
            Path filePath = searchPath.resolve(relativePath);
            if (Files.exists(filePath)) {
                return extractMethodSource(filePath, methodName);
            }
        }
        return null;
    }

    private String extractMethodSource(Path filePath, String methodName) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(filePath);

            // 모든 메서드에서 이름이 일치하는 것 찾기
            return cu.findAll(MethodDeclaration.class).stream()
                    .filter(m -> m.getNameAsString().equals(methodName))
                    .findFirst()
                    .map(MethodDeclaration::toString)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
```

---

### Domain

#### TestNode

테스트 카탈로그의 노드. JUnit 계층 구조를 표현.

```java
@Getter
@Builder
@EqualsAndHashCode(of = {"uniqueId"})
public class TestNode {
    private final String uniqueId;        // JUnit UniqueId
    private final String parentUniqueId;  // 부모 ID (트리 구조용)
    private final String displayName;     // 표시 이름 (@DisplayName)
    private final String className;       // 테스트 클래스명
    private final String type;            // "CONTAINER" or "TEST"
}
```

---

#### TestExecution

테스트 실행 이력.

```java
@Getter
@Builder
public class TestExecution {
    private String executionId;           // UUID
    private LocalDateTime startedAt;      // 시작 시간
    private LocalDateTime finishedAt;     // 종료 시간
    private int totalTests;               // 전체 테스트 수
    private int successCount;             // 성공 수
    private int failedCount;              // 실패 수
    private int skippedCount;             // 스킵 수
    private long totalDurationMillis;     // 총 소요 시간
    private String requesterIp;           // 요청자 IP
    private String classNames;            // 실행한 클래스들 (쉼표 구분)
    private String status;                // "RUNNING" or "COMPLETED"
}
```

---

#### TestResult

테스트 실행 결과 (트리 구조).

```java
@Getter @Setter
@EqualsAndHashCode(of = {"id", "displayName"})
public class TestResult {
    private final String id;              // JUnit UniqueId
    private final String displayName;     // 표시 이름
    private TestStatus status;            // 상태
    private long durationMillis;          // 소요 시간
    private String errorMessage;          // 에러 메시지
    private String stackTrace;            // 스택 트레이스
    private String stdout;                // 표준 출력 (캡처됨)
    private List<TestResult> children;    // 자식 결과들

    public void addChild(TestResult child) {
        this.children.add(child);
    }
}
```

---

#### TestStatus

테스트 상태 enum.

```java
public enum TestStatus {
    RUNNING,   // 실행 중
    SUCCESS,   // 성공
    FAILED,    // 실패
    SKIPPED    // 스킵
}
```

---

### DTO

#### TreeNodeDto

사이드바 트리용 DTO.

```java
@Getter
@Builder
public class TreeNodeDto {
    private String name;              // 노드명 (패키지명 또는 클래스명)
    private String uniqueId;          // JUnit UniqueId
    private String parentUniqueId;    // 부모 ID
    private String className;         // 전체 클래스명
    private NodeType type;            // PACKAGE or CLASS
    private List<TreeNodeDto> children;

    public enum NodeType {
        PACKAGE,
        CLASS
    }
}
```

---

#### TestExecutionRequest

테스트 실행 요청.

```java
@Getter
@Setter
public class TestExecutionRequest {
    @NotNull(message = "Class names cannot be null")
    @NotEmpty(message = "At least one class name is required")
    private List<String> classNames;
}
```

---

## 데이터베이스

### 스키마: `bng000a`

### 1. c_test_node_catalog

테스트 카탈로그 저장. JUnit 계층 구조를 평탄화하여 저장.

```sql
CREATE TABLE bng000a.C_TEST_NODE_CATALOG (
    unique_id        VARCHAR(200) NOT NULL PRIMARY KEY,  -- JUnit UniqueId
    parent_unique_id VARCHAR(200),                       -- 부모 노드 ID
    displayname      VARCHAR(200),                       -- @DisplayName 값
    classname        VARCHAR(200),                       -- 테스트 클래스명
    type             VARCHAR(20),                        -- CONTAINER or TEST
    updatedat        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                     ON UPDATE CURRENT_TIMESTAMP         -- 업데이트 시간
);
```

**예시 데이터:**
```
unique_id: [engine:junit-jupiter]/[class:testauto.testcode.unit.SampleTest]
parent_unique_id: [engine:junit-jupiter]
displayname: SampleTest
classname: testauto.testcode.unit.SampleTest
type: CONTAINER

unique_id: [engine:junit-jupiter]/[class:...SampleTest]/[method:successTest()]
parent_unique_id: [engine:junit-jupiter]/[class:...SampleTest]
displayname: 성공 테스트 입니다
classname: testauto.testcode.unit.SampleTest
type: TEST
```

---

### 2. c_test_execution

테스트 실행 이력 저장.

```sql
CREATE TABLE bng000a.c_test_execution (
    execution_id          VARCHAR(36) PRIMARY KEY,       -- UUID
    started_at            DATETIME,                      -- 실행 시작 시간
    finished_at           DATETIME,                      -- 실행 종료 시간
    total_tests           INT DEFAULT 0,                 -- 전체 테스트 수
    success_count         INT DEFAULT 0,                 -- 성공 수
    failed_count          INT DEFAULT 0,                 -- 실패 수
    skipped_count         INT DEFAULT 0,                 -- 스킵 수
    total_duration_millis BIGINT DEFAULT 0,              -- 총 소요 시간 (ms)
    requester_ip          VARCHAR(45),                   -- 요청자 IP
    class_names           TEXT,                          -- 실행한 클래스들
    status                VARCHAR(20) DEFAULT 'RUNNING', -- RUNNING or COMPLETED

    INDEX c_test_execution_idx1 (started_at DESC),
    INDEX c_test_execution_idx2 (status)
);
```

---

### 3. c_test_result

테스트 실행 결과 상세 저장. 트리 구조 지원.

```sql
CREATE TABLE bng000a.c_test_result (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,  -- 자동 증가 ID
    execution_id     VARCHAR(36) NOT NULL,               -- 실행 ID (FK)
    test_id          VARCHAR(1000) NOT NULL,             -- 테스트 UniqueId
    parent_test_id   VARCHAR(1000),                      -- 부모 테스트 ID
    display_name     VARCHAR(500),                       -- 표시 이름
    status           VARCHAR(20) NOT NULL,               -- SUCCESS/FAILED/SKIPPED
    duration_millis  BIGINT DEFAULT 0,                   -- 소요 시간 (ms)
    error_message    TEXT,                               -- 에러 메시지
    stack_trace      TEXT,                               -- 스택 트레이스
    stdout           TEXT,                               -- 캡처된 stdout

    INDEX c_test_result_idx1 (execution_id),
    FOREIGN KEY (execution_id)
        REFERENCES bng000a.c_test_execution(execution_id)
        ON DELETE CASCADE
);
```

---

## 설정

### application.yml

```yaml
spring:
  application:
    name: testautomation

  thymeleaf:
    cache: false               # 개발 시 템플릿 캐시 비활성화

  datasource:
    url: jdbc:mysql://localhost:3306?useSSL=false&serverTimezone=Asia/Seoul
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 100
      minimum-idle: 5

server:
  port: 9898

testcode:
  project-path: ${TESTCODE_PROJECT_PATH:/opt/testcodes}   # 테스트 코드 프로젝트 경로
  root-package: ${TESTCODE_ROOT_PACKAGE:testauto.testcode} # 테스트 루트 패키지

logging:
  level:
    org.springframework.jdbc.core: debug
```

### 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `TESTCODE_PROJECT_PATH` | 테스트 코드 프로젝트 경로 | `/opt/testcodes` |
| `TESTCODE_ROOT_PACKAGE` | 테스트 루트 패키지 | `testauto.testcode` |

---

## 실행 방법

### 1. 데이터베이스 준비

```sql
CREATE DATABASE bng000a;

-- DDL 실행 (sql/ 디렉토리 참고)
```

### 2. 테스트 코드 프로젝트 준비

```bash
# 템플릿 복사
cp -r testcode-template /opt/testcodes

# 또는 별도 Git 저장소로 관리
cd /opt/testcodes
git init
git add .
git commit -m "Initial commit"
```

### 3. 애플리케이션 실행

```bash
# 개발 환경
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew bootJar
java -jar build/libs/testautomation-0.0.1-SNAPSHOT.jar

# 환경 변수로 경로 지정
TESTCODE_PROJECT_PATH=/path/to/testcodes java -jar app.jar
```

### 4. 접속

브라우저에서 `http://localhost:9898` 접속

---

## 워크플로우

### 테스트 코드 개발 → 실행 흐름

```
[개발자 PC]
    │
    │  1. 테스트 코드 수정
    │  2. git push
    │
    ▼
[서버: /opt/testcodes]
    │
    │  3. git pull (수동 또는 webhook)
    │
    ▼
[웹 UI: 새로고침 버튼 클릭]
    │
    │  4. POST /api/tests/refresh
    │     → gradle compileJava
    │     → 별도 JVM에서 discover
    │     → DB 저장
    │
    ▼
[웹 UI: 테스트 선택 → 실행 버튼 클릭]
    │
    │  5. POST /api/tests/run
    │     → executionId 즉시 반환
    │     → 비동기로 별도 JVM에서 실행
    │     → 결과 DB 저장
    │
    ▼
[웹 UI: 결과 조회]
    │
    │  6. GET /api/tests/executions/{id}/results
    │     → 트리 형태 결과 + 요약 정보
    │
    ▼
[결과 확인: 성공/실패/stdout/에러 등]
```
