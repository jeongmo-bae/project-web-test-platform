# JUnit Platform 가이드

이 프로젝트에서 사용하는 JUnit Platform 관련 개념과 API 정리.

---

## 목차

1. [JUnit 5 아키텍처](#junit-5-아키텍처)
2. [JUnit Platform 핵심 개념](#junit-platform-핵심-개념)
3. [Launcher API](#launcher-api)
4. [TestIdentifier](#testidentifier)
5. [TestPlan](#testplan)
6. [TestExecutionListener](#testexecutionlistener)
7. [DiscoverySelectors](#discoveryselectors)
8. [TestExecutionResult](#testexecutionresult)
9. [이 프로젝트에서의 활용](#이-프로젝트에서의-활용)

---

## JUnit 5 아키텍처

### JUnit 5 = Platform + Jupiter + Vintage

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              JUnit 5                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │  JUnit Jupiter  │  │  JUnit Vintage  │  │   3rd Party Engines     │  │
│  │                 │  │                 │  │                         │  │
│  │  - @Test        │  │  - JUnit 3 지원  │  │  - Spock               │  │
│  │  - @DisplayName │  │  - JUnit 4 지원  │  │  - Cucumber            │  │
│  │  - @Nested      │  │                 │  │  - etc.                 │  │
│  │  - @BeforeEach  │  │                 │  │                         │  │
│  │  - Assertions   │  │                 │  │                         │  │
│  └────────┬────────┘  └────────┬────────┘  └────────────┬────────────┘  │
│           │                    │                        │               │
│           └────────────────────┼────────────────────────┘               │
│                                │                                        │
│                                ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      JUnit Platform                              │   │
│  │                                                                  │   │
│  │  - Launcher API (테스트 발견 & 실행)                              │   │
│  │  - TestEngine SPI (테스트 엔진 인터페이스)                         │   │
│  │  - Console Launcher (CLI 실행)                                   │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                │                                        │
│                                ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                         IDE / Build Tool                         │   │
│  │              (IntelliJ, Eclipse, Gradle, Maven)                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 각 모듈 역할

| 모듈 | 역할 | Maven Artifact |
|------|------|----------------|
| **JUnit Platform** | 테스트 실행 기반 제공 (Launcher, TestEngine SPI) | `junit-platform-launcher` |
| **JUnit Jupiter** | JUnit 5 스타일 테스트 작성 (@Test, @Nested 등) | `junit-jupiter-api`, `junit-jupiter-engine` |
| **JUnit Vintage** | JUnit 3/4 테스트 실행 지원 | `junit-vintage-engine` |

### 의존성 구조

```
junit-platform-launcher
    └── junit-platform-engine
            └── junit-platform-commons

junit-jupiter-engine
    ├── junit-jupiter-api
    └── junit-platform-engine

junit-vintage-engine
    ├── junit:junit (JUnit 4)
    └── junit-platform-engine
```

### 이 프로젝트에서 사용하는 의존성

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.junit.platform:junit-platform-launcher")  // Launcher API
    implementation("org.junit.jupiter:junit-jupiter-engine")      // Jupiter 엔진
}
```

---

## JUnit Platform 핵심 개념

### 전체 흐름

```
┌──────────────────────────────────────────────────────────────────────┐
│                        테스트 실행 흐름                               │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   1. Discovery (발견)                                                │
│   ┌────────────────────────────────────────────────────────────┐    │
│   │  LauncherDiscoveryRequest                                   │    │
│   │    - 어떤 패키지/클래스를 스캔할지                            │    │
│   │    - 필터 조건                                              │    │
│   │                     │                                       │    │
│   │                     ▼                                       │    │
│   │               ┌──────────┐                                  │    │
│   │               │ Launcher │                                  │    │
│   │               └────┬─────┘                                  │    │
│   │                    │ discover()                             │    │
│   │                    ▼                                       │    │
│   │               ┌──────────┐                                  │    │
│   │               │ TestPlan │  ← 발견된 테스트 트리              │    │
│   │               └──────────┘                                  │    │
│   └────────────────────────────────────────────────────────────┘    │
│                                                                      │
│   2. Execution (실행)                                                │
│   ┌────────────────────────────────────────────────────────────┐    │
│   │               ┌──────────┐                                  │    │
│   │               │ Launcher │                                  │    │
│   │               └────┬─────┘                                  │    │
│   │                    │ execute()                              │    │
│   │                    ▼                                       │    │
│   │           ┌────────────────────┐                            │    │
│   │           │ TestExecutionListener │  ← 이벤트 수신          │    │
│   │           └────────────────────┘                            │    │
│   │                    │                                        │    │
│   │        ┌───────────┼───────────┐                            │    │
│   │        ▼           ▼           ▼                            │    │
│   │   Started     Finished     Skipped                          │    │
│   │   (시작)       (완료)       (스킵)                           │    │
│   └────────────────────────────────────────────────────────────┘    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 핵심 클래스/인터페이스 관계

```
Launcher
    │
    ├── discover(LauncherDiscoveryRequest) → TestPlan
    │                                           │
    │                                           └── TestIdentifier (트리 노드)
    │
    └── execute(LauncherDiscoveryRequest)
                    │
                    └── TestExecutionListener
                            │
                            ├── executionStarted(TestIdentifier)
                            ├── executionFinished(TestIdentifier, TestExecutionResult)
                            └── executionSkipped(TestIdentifier, String reason)
```

---

## Launcher API

### Launcher

테스트 발견과 실행의 진입점. `LauncherFactory`로 생성.

```java
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherFactory;

// Launcher 생성
Launcher launcher = LauncherFactory.create();

// 테스트 발견
TestPlan testPlan = launcher.discover(request);

// 테스트 실행
launcher.execute(request);
```

### LauncherDiscoveryRequest

테스트 발견/실행 요청. 어떤 테스트를 대상으로 할지 정의.

```java
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.engine.discovery.DiscoverySelectors;

// 패키지 기반 요청
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(
            DiscoverySelectors.selectPackage("com.example.tests")
        )
        .build();

// 클래스 기반 요청
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(
            DiscoverySelectors.selectClass("com.example.tests.MyTest"),
            DiscoverySelectors.selectClass("com.example.tests.OtherTest")
        )
        .build();

// 필터 추가
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(DiscoverySelectors.selectPackage("com.example"))
        .filters(
            ClassNameFilter.includeClassNamePatterns(".*Test"),
            TagFilter.includeTags("fast")
        )
        .build();
```

### LauncherDiscoveryRequestBuilder 메서드

| 메서드 | 설명 |
|--------|------|
| `selectors(DiscoverySelector...)` | 테스트 선택 기준 추가 |
| `filters(Filter...)` | 필터 조건 추가 |
| `configurationParameter(key, value)` | 설정 파라미터 추가 |
| `configurationParameters(Map)` | 설정 파라미터 일괄 추가 |
| `build()` | 요청 객체 생성 |

---

## TestIdentifier

### 개요

테스트 트리의 노드를 표현. 클래스, 메서드, Nested 클래스 등 모든 테스트 요소.

```
TestPlan (테스트 계획)
    │
    └── TestIdentifier (JUnit Jupiter 엔진 루트)
            │
            ├── TestIdentifier (테스트 클래스: SampleTest)
            │       │
            │       ├── TestIdentifier (메서드: successTest)
            │       ├── TestIdentifier (메서드: failedTest)
            │       │
            │       └── TestIdentifier (Nested 클래스: NestedGroup)
            │               │
            │               ├── TestIdentifier (메서드: nestedTest1)
            │               └── TestIdentifier (메서드: nestedTest2)
            │
            └── TestIdentifier (테스트 클래스: OtherTest)
                    │
                    └── ...
```

### 주요 메서드

```java
public interface TestIdentifier {

    // 고유 식별자 (전체 경로)
    String getUniqueId();
    // 예: "[engine:junit-jupiter]/[class:com.example.SampleTest]/[method:successTest()]"

    // 부모 ID (Optional)
    Optional<String> getParentId();
    // 예: "[engine:junit-jupiter]/[class:com.example.SampleTest]"

    // 표시 이름 (@DisplayName 또는 메서드명)
    String getDisplayName();
    // 예: "성공 테스트 입니다" 또는 "successTest()"

    // 테스트 소스 정보 (Optional)
    Optional<TestSource> getSource();
    // ClassSource, MethodSource 등

    // 컨테이너 여부 (클래스, Nested 클래스 등)
    boolean isContainer();

    // 테스트 여부 (실제 실행되는 테스트 메서드)
    boolean isTest();

    // 타입 (CONTAINER, TEST, CONTAINER_AND_TEST)
    Type getType();

    // 레거시 보고용 이름
    String getLegacyReportingName();
}
```

### UniqueId 구조

```
UniqueId 형식:
[segment-type:segment-value]/[segment-type:segment-value]/...

예시:
[engine:junit-jupiter]
    └── 엔진 타입: junit-jupiter

[engine:junit-jupiter]/[class:com.example.SampleTest]
    └── 클래스: com.example.SampleTest

[engine:junit-jupiter]/[class:com.example.SampleTest]/[method:successTest()]
    └── 메서드: successTest()

[engine:junit-jupiter]/[class:com.example.SampleTest]/[nested-class:NestedGroup]
    └── Nested 클래스: NestedGroup

[engine:junit-jupiter]/[class:com.example.SampleTest]/[nested-class:NestedGroup]/[method:nestedTest()]
    └── Nested 클래스 내 메서드: nestedTest()
```

### Type enum

```java
public enum Type {
    CONTAINER,          // 컨테이너 (클래스, Nested 클래스)
    TEST,               // 테스트 (메서드)
    CONTAINER_AND_TEST  // 둘 다 (드문 경우)
}
```

### TestSource 종류

```java
// ClassSource - 클래스 정보
ClassSource classSource = (ClassSource) testIdentifier.getSource().get();
String className = classSource.getClassName();
// "com.example.SampleTest"

// MethodSource - 메서드 정보
MethodSource methodSource = (MethodSource) testIdentifier.getSource().get();
String className = methodSource.getClassName();
String methodName = methodSource.getMethodName();
// "com.example.SampleTest", "successTest"

// 소스 타입 체크
testIdentifier.getSource().ifPresent(source -> {
    if (source instanceof ClassSource cs) {
        System.out.println("Class: " + cs.getClassName());
    } else if (source instanceof MethodSource ms) {
        System.out.println("Method: " + ms.getMethodName());
    }
});
```

### 이 프로젝트에서의 사용

```java
// TestRunner.java - 테스트 발견 시 노드 수집
private static void collectNodes(TestPlan testPlan, TestIdentifier testIdentifier,
                                 List<TestNodeDto> nodes) {
    String uniqueId = testIdentifier.getUniqueId();
    String displayName = testIdentifier.getDisplayName();
    String parentId = testIdentifier.getParentId().orElse(null);

    // 클래스 소스에서 클래스명 추출
    TestSource source = testIdentifier.getSource().orElse(null);
    String className = null;
    if (source instanceof ClassSource classSource) {
        className = classSource.getClassName();
    }

    // 타입 결정
    String nodeType = testIdentifier.isContainer() ? "CONTAINER" : "TEST";

    nodes.add(new TestNodeDto(uniqueId, parentId, displayName, className, nodeType));

    // 자식 노드 재귀 처리
    for (TestIdentifier child : testPlan.getChildren(testIdentifier)) {
        collectNodes(testPlan, child, nodes);
    }
}
```

---

## TestPlan

### 개요

테스트 발견 결과. 모든 TestIdentifier를 트리 구조로 보관.

```java
import org.junit.platform.launcher.TestPlan;

// 테스트 발견
TestPlan testPlan = launcher.discover(request);

// 루트 노드들 (보통 엔진 노드)
Set<TestIdentifier> roots = testPlan.getRoots();

// 특정 노드의 자식들
Set<TestIdentifier> children = testPlan.getChildren(parentIdentifier);

// 특정 노드의 자식들 (ID로)
Set<TestIdentifier> children = testPlan.getChildren(parentUniqueId);

// 특정 노드의 부모
Optional<TestIdentifier> parent = testPlan.getParent(testIdentifier);

// 모든 루트 포함 여부
boolean contains = testPlan.containsTests();
```

### 주요 메서드

| 메서드 | 반환 타입 | 설명 |
|--------|----------|------|
| `getRoots()` | `Set<TestIdentifier>` | 루트 노드들 (보통 엔진) |
| `getChildren(TestIdentifier)` | `Set<TestIdentifier>` | 특정 노드의 자식들 |
| `getChildren(String uniqueId)` | `Set<TestIdentifier>` | ID로 자식 조회 |
| `getParent(TestIdentifier)` | `Optional<TestIdentifier>` | 부모 노드 |
| `getDescendants(TestIdentifier)` | `Set<TestIdentifier>` | 모든 자손 노드 |
| `containsTests()` | `boolean` | 테스트 포함 여부 |
| `countTestIdentifiers(Predicate)` | `long` | 조건에 맞는 노드 수 |

### 트리 순회 예시

```java
// 모든 테스트 노드 출력
private void printTestTree(TestPlan testPlan) {
    for (TestIdentifier root : testPlan.getRoots()) {
        printNode(testPlan, root, 0);
    }
}

private void printNode(TestPlan testPlan, TestIdentifier node, int depth) {
    String indent = "  ".repeat(depth);
    String type = node.isContainer() ? "[C]" : "[T]";
    System.out.println(indent + type + " " + node.getDisplayName());

    for (TestIdentifier child : testPlan.getChildren(node)) {
        printNode(testPlan, child, depth + 1);
    }
}

// 출력 예:
// [C] JUnit Jupiter
//   [C] SampleTest
//     [T] 성공 테스트 입니다
//     [T] 실패 테스트 입니다
//     [C] Nested Group 테스트 입니다
//       [T] Nested1 테스트 입니다
//       [T] Nested2 테스트 입니다
```

---

## TestExecutionListener

### 개요

테스트 실행 이벤트를 수신하는 리스너. 테스트 시작, 완료, 스킵 등 이벤트 처리.

```java
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.engine.TestExecutionResult;

public class MyTestListener implements TestExecutionListener {

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        // 테스트 시작 시 호출
        System.out.println("Started: " + testIdentifier.getDisplayName());
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
                                  TestExecutionResult result) {
        // 테스트 완료 시 호출
        System.out.println("Finished: " + testIdentifier.getDisplayName()
                         + " - " + result.getStatus());
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        // 테스트 스킵 시 호출
        System.out.println("Skipped: " + testIdentifier.getDisplayName()
                         + " - " + reason);
    }
}
```

### 전체 메서드

```java
public interface TestExecutionListener {

    // 테스트 계획 시작 전
    default void testPlanExecutionStarted(TestPlan testPlan) {}

    // 테스트 계획 완료 후
    default void testPlanExecutionFinished(TestPlan testPlan) {}

    // 동적 테스트 등록 시
    default void dynamicTestRegistered(TestIdentifier testIdentifier) {}

    // 테스트 실행 스킵 시 (조건부 비활성화 등)
    default void executionSkipped(TestIdentifier testIdentifier, String reason) {}

    // 테스트 실행 시작 시
    default void executionStarted(TestIdentifier testIdentifier) {}

    // 테스트 실행 완료 시
    default void executionFinished(TestIdentifier testIdentifier,
                                   TestExecutionResult result) {}

    // 리포팅 엔트리 발행 시
    default void reportingEntryPublished(TestIdentifier testIdentifier,
                                         ReportEntry entry) {}
}
```

### 이벤트 호출 순서

```
testPlanExecutionStarted(TestPlan)
    │
    ├── executionStarted(TestIdentifier: 엔진)
    │       │
    │       ├── executionStarted(TestIdentifier: 클래스)
    │       │       │
    │       │       ├── executionStarted(TestIdentifier: 메서드)
    │       │       ├── executionFinished(TestIdentifier: 메서드, SUCCESSFUL)
    │       │       │
    │       │       ├── executionStarted(TestIdentifier: 메서드)
    │       │       ├── executionFinished(TestIdentifier: 메서드, FAILED)
    │       │       │
    │       │       ├── executionSkipped(TestIdentifier: 메서드, "Disabled")
    │       │       │
    │       │       └── ...
    │       │
    │       └── executionFinished(TestIdentifier: 클래스, SUCCESSFUL)
    │
    └── executionFinished(TestIdentifier: 엔진, SUCCESSFUL)
    │
testPlanExecutionFinished(TestPlan)
```

### 리스너 등록

```java
Launcher launcher = LauncherFactory.create();

// 리스너 등록
launcher.registerTestExecutionListeners(new MyTestListener());

// 테스트 실행 (리스너에 이벤트 전달됨)
launcher.execute(request);
```

### 이 프로젝트에서의 구현

```java
// TestRunnerListener.java
public class TestRunnerListener implements TestExecutionListener {

    private final Map<String, MutableTestResult> nodeMap = new ConcurrentHashMap<>();
    private final PrintStream originalOut = System.out;
    private final Map<String, ByteArrayOutputStream> stdoutCaptures = new ConcurrentHashMap<>();

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        String id = testIdentifier.getUniqueId();

        // 결과 노드 생성
        MutableTestResult node = new MutableTestResult(id, testIdentifier.getDisplayName());
        nodeMap.put(id, node);

        // 부모-자식 관계 설정
        testIdentifier.getParentId().ifPresent(parentId -> {
            MutableTestResult parent = nodeMap.get(parentId);
            if (parent != null) {
                parent.children.add(node);
            }
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

        // 상태 저장
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

        // stdout 캡처 종료 및 저장
        if (testIdentifier.isTest()) {
            System.setOut(originalOut);
            ByteArrayOutputStream baos = stdoutCaptures.remove(id);
            if (baos != null) {
                node.stdout = baos.toString();
            }
        }
    }
}
```

---

## DiscoverySelectors

### 개요

테스트 발견 대상을 지정하는 셀렉터. 패키지, 클래스, 메서드 등 다양한 방식 지원.

```java
import org.junit.platform.engine.discovery.DiscoverySelectors;
```

### 셀렉터 종류

| 메서드 | 설명 | 예시 |
|--------|------|------|
| `selectPackage(String)` | 패키지 선택 | `selectPackage("com.example.tests")` |
| `selectClass(String)` | 클래스명으로 선택 | `selectClass("com.example.MyTest")` |
| `selectClass(Class<?>)` | 클래스 객체로 선택 | `selectClass(MyTest.class)` |
| `selectMethod(String)` | 메서드 선택 (전체 경로) | `selectMethod("com.example.MyTest#testMethod")` |
| `selectMethod(Class<?>, String)` | 클래스와 메서드명으로 선택 | `selectMethod(MyTest.class, "testMethod")` |
| `selectClasspathRoots(Set<Path>)` | 클래스패스 루트 선택 | `selectClasspathRoots(Set.of(Path.of("build/classes")))` |
| `selectClasspathResource(String)` | 클래스패스 리소스 선택 | `selectClasspathResource("test-data.csv")` |
| `selectDirectory(String)` | 디렉토리 선택 | `selectDirectory("src/test/java")` |
| `selectFile(String)` | 파일 선택 | `selectFile("src/test/java/MyTest.java")` |
| `selectModule(String)` | 모듈 선택 (Java 9+) | `selectModule("my.module")` |
| `selectUniqueId(String)` | UniqueId로 선택 | `selectUniqueId("[engine:junit-jupiter]/[class:...]")` |
| `selectUri(URI)` | URI로 선택 | `selectUri(URI.create("classpath:/tests"))` |

### 사용 예시

```java
// 1. 패키지 전체 테스트
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(DiscoverySelectors.selectPackage("testauto.testcode"))
        .build();

// 2. 특정 클래스들
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(
            DiscoverySelectors.selectClass("testauto.testcode.unit.SampleTest"),
            DiscoverySelectors.selectClass("testauto.testcode.e2e.EbmTest")
        )
        .build();

// 3. 특정 메서드만
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(
            DiscoverySelectors.selectMethod(
                "testauto.testcode.unit.SampleTest#successTest")
        )
        .build();

// 4. UniqueId로 선택 (이전 실행에서 실패한 테스트 재실행 등)
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(
            DiscoverySelectors.selectUniqueId(
                "[engine:junit-jupiter]/[class:testauto.testcode.unit.SampleTest]/[method:failedTest()]")
        )
        .build();
```

### 이 프로젝트에서의 사용

```java
// TestRunner.java - 패키지 기반 발견
private static void runDiscover(String rootPackage) {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectPackage(rootPackage))
            .build();

    TestPlan testPlan = LauncherFactory.create().discover(request);
    // ...
}

// TestRunner.java - 클래스 기반 실행
private static void runTests(List<String> classNames) {
    List<DiscoverySelector> selectors = classNames.stream()
            .map(DiscoverySelectors::selectClass)
            .toList();

    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectors)
            .build();

    LauncherFactory.create().execute(request);
    // ...
}
```

---

## TestExecutionResult

### 개요

테스트 실행 결과. 성공/실패/중단 상태와 예외 정보 포함.

```java
import org.junit.platform.engine.TestExecutionResult;
```

### 상태 (Status)

```java
public enum Status {
    SUCCESSFUL,  // 테스트 성공
    FAILED,      // 테스트 실패 (assertion 실패, 예외 발생)
    ABORTED      // 테스트 중단 (assumption 실패, @Disabled 등)
}
```

### 주요 메서드

```java
public interface TestExecutionResult {

    // 상태 조회
    Status getStatus();

    // 예외 조회 (실패/중단 시)
    Optional<Throwable> getThrowable();

    // 정적 팩토리 메서드
    static TestExecutionResult successful();
    static TestExecutionResult failed(Throwable throwable);
    static TestExecutionResult aborted(Throwable throwable);
}
```

### 사용 예시

```java
@Override
public void executionFinished(TestIdentifier testIdentifier,
                              TestExecutionResult result) {

    // 상태 확인
    switch (result.getStatus()) {
        case SUCCESSFUL -> handleSuccess(testIdentifier);
        case FAILED -> handleFailure(testIdentifier, result.getThrowable().orElse(null));
        case ABORTED -> handleAborted(testIdentifier, result.getThrowable().orElse(null));
    }

    // 예외 정보 추출
    result.getThrowable().ifPresent(throwable -> {
        String message = throwable.getMessage();
        String stackTrace = getStackTraceAsString(throwable);

        // 에러 유형 확인
        if (throwable instanceof AssertionError) {
            // assertion 실패
        } else if (throwable instanceof TestAbortedException) {
            // assumption 실패
        } else {
            // 일반 예외
        }
    });
}

private String getStackTraceAsString(Throwable t) {
    StringBuilder sb = new StringBuilder();
    sb.append(t.toString()).append("\n");
    for (StackTraceElement element : t.getStackTrace()) {
        sb.append("\tat ").append(element).append("\n");
    }
    return sb.toString();
}
```

### 상태별 의미

| 상태 | 의미 | 발생 조건 |
|------|------|----------|
| `SUCCESSFUL` | 테스트 성공 | 예외 없이 정상 완료 |
| `FAILED` | 테스트 실패 | `AssertionError`, `Exception` 발생 |
| `ABORTED` | 테스트 중단 | `@Disabled`, `Assumptions.assumeTrue()` 실패 |

---

## 이 프로젝트에서의 활용

### 전체 흐름

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         이 프로젝트의 JUnit Platform 활용               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. 테스트 발견 (POST /api/tests/refresh)                               │
│  ┌───────────────────────────────────────────────────────────────┐     │
│  │  TestRunner.runDiscover("testauto.testcode")                  │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  LauncherDiscoveryRequest                                     │     │
│  │      .selectPackage("testauto.testcode")                     │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  Launcher.discover() → TestPlan                               │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  TestPlan.getRoots() → TestIdentifier 트리 순회               │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  각 TestIdentifier에서 정보 추출                              │     │
│  │      - uniqueId                                               │     │
│  │      - parentUniqueId                                         │     │
│  │      - displayName                                            │     │
│  │      - className (ClassSource에서)                            │     │
│  │      - type (CONTAINER/TEST)                                  │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  JSON으로 변환 → stdout 출력 → 메인 앱에서 파싱 → DB 저장     │     │
│  └───────────────────────────────────────────────────────────────┘     │
│                                                                         │
│  2. 테스트 실행 (POST /api/tests/run)                                   │
│  ┌───────────────────────────────────────────────────────────────┐     │
│  │  TestRunner.runTests(["testauto.testcode.unit.SampleTest"])   │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  LauncherDiscoveryRequest                                     │     │
│  │      .selectClass("testauto.testcode.unit.SampleTest")       │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  TestRunnerListener 등록                                      │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  Launcher.execute()                                           │     │
│  │      │                                                        │     │
│  │      ├── executionStarted(TestIdentifier)                     │     │
│  │      │       - 노드 생성                                      │     │
│  │      │       - stdout 캡처 시작 (테스트 메서드만)              │     │
│  │      │                                                        │     │
│  │      └── executionFinished(TestIdentifier, TestExecutionResult)│    │
│  │              - 상태 저장 (SUCCESS/FAILED/SKIPPED)             │     │
│  │              - 소요 시간 계산                                  │     │
│  │              - 에러 메시지/스택트레이스 저장                   │     │
│  │              - stdout 캡처 종료 및 저장                       │     │
│  │      │                                                        │     │
│  │      ▼                                                        │     │
│  │  결과 트리 + 요약 정보 JSON 출력 → 메인 앱에서 파싱 → DB 저장 │     │
│  └───────────────────────────────────────────────────────────────┘     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 코드 위치

| 클래스 | 위치 | JUnit Platform 사용 |
|--------|------|---------------------|
| `TestRunner` | `testauto/runner/` | Launcher, DiscoverySelectors, TestPlan |
| `TestRunnerListener` | `testauto/runner/` | TestExecutionListener, TestIdentifier, TestExecutionResult |
| `WebTestListener` | `testauto/util/junit/` | (레거시) TestExecutionListener |

### 핵심 코드 요약

```java
// === 테스트 발견 ===
Launcher launcher = LauncherFactory.create();

LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(DiscoverySelectors.selectPackage("testauto.testcode"))
        .build();

TestPlan testPlan = launcher.discover(request);

for (TestIdentifier root : testPlan.getRoots()) {
    collectNodes(testPlan, root, nodes);  // 재귀적으로 트리 순회
}


// === 테스트 실행 ===
Launcher launcher = LauncherFactory.create();

TestRunnerListener listener = new TestRunnerListener();
launcher.registerTestExecutionListeners(listener);

List<DiscoverySelector> selectors = classNames.stream()
        .map(DiscoverySelectors::selectClass)
        .toList();

LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectors)
        .build();

launcher.execute(request);  // 리스너에 이벤트 전달됨

List<TestResultDto> results = listener.findRootResults();
TestSummaryDto summary = listener.buildSummary();
```

---

## 참고 자료

### 공식 문서
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [JUnit Platform Launcher API](https://junit.org/junit5/docs/current/user-guide/#launcher-api)
- [JUnit 5 Architecture](https://junit.org/junit5/docs/current/user-guide/#overview-what-is-junit-5)

### Maven 의존성
```xml
<!-- JUnit Platform Launcher -->
<dependency>
    <groupId>org.junit.platform</groupId>
    <artifactId>junit-platform-launcher</artifactId>
    <version>1.10.2</version>
</dependency>

<!-- JUnit Jupiter Engine -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>5.10.2</version>
</dependency>
```

### Gradle 의존성
```kotlin
dependencies {
    implementation("org.junit.platform:junit-platform-launcher:1.10.2")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}
```
