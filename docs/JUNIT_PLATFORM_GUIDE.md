# JUnit Platform Launcher API 가이드

이 문서는 Web Test Platform에서 JUnit Platform Launcher API를 어떻게 활용하는지 설명합니다.

---

## 1. JUnit 5 아키텍처 개요

JUnit 5는 세 가지 주요 모듈로 구성됩니다:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              JUnit 5                                         │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         JUnit Platform                               │   │
│  │                                                                      │   │
│  │   - 테스트 발견 및 실행을 위한 기반 플랫폼                            │   │
│  │   - Launcher API 제공                                                │   │
│  │   - IDE 및 빌드 도구와의 통합 지점                                    │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│                    ┌───────────────┼───────────────┐                        │
│                    ▼               ▼               ▼                        │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐            │
│  │  JUnit Jupiter   │ │  JUnit Vintage   │ │   Third-Party    │            │
│  │                  │ │                  │ │     Engines      │            │
│  │  - JUnit 5 API   │ │  - JUnit 3/4     │ │  - TestNG        │            │
│  │  - @Test, @Before│ │    하위 호환      │ │  - Cucumber      │            │
│  │    All 등        │ │                  │ │  - Spock 등      │            │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 핵심 클래스 및 인터페이스

### 2.1 Launcher

테스트 발견 및 실행의 진입점입니다.

```java
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;

// Launcher 인스턴스 생성
Launcher launcher = LauncherFactory.create();

// 테스트 발견
TestPlan testPlan = launcher.discover(request);

// 테스트 실행
launcher.execute(request);
```

### 2.2 LauncherDiscoveryRequest

어떤 테스트를 발견할지 정의합니다.

```java
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
    // 선택자 (Selector) - 무엇을 발견할지
    .selectors(
        selectPackage("com.example.tests"),        // 패키지 선택
        selectClass("com.example.MyTest"),         // 클래스 선택
        selectMethod("com.example.MyTest#testMethod") // 메서드 선택
    )
    // 필터 (Filter) - 조건 적용
    .filters(
        includeClassNamePatterns(".*Test"),        // 클래스명 패턴
        includeTags("fast")                        // 태그 필터
    )
    .build();
```

### 2.3 TestPlan

발견된 테스트의 계층 구조를 담고 있습니다.

```java
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.TestIdentifier;

TestPlan testPlan = launcher.discover(request);

// 루트 노드들 조회
Set<TestIdentifier> roots = testPlan.getRoots();

// 자식 노드들 조회
Set<TestIdentifier> children = testPlan.getChildren(parent);

// 모든 테스트 조회
testPlan.getDescendants(root).forEach(testIdentifier -> {
    System.out.println(testIdentifier.getDisplayName());
});
```

### 2.4 TestIdentifier

개별 테스트 또는 컨테이너를 식별합니다.

```java
import org.junit.platform.launcher.TestIdentifier;

TestIdentifier identifier = ...;

// 고유 ID
String uniqueId = identifier.getUniqueId();
// 예: [engine:junit-jupiter]/[class:com.example.MyTest]/[method:testMethod()]

// 표시 이름
String displayName = identifier.getDisplayName();

// 테스트인지 컨테이너인지
boolean isTest = identifier.isTest();
boolean isContainer = identifier.isContainer();

// 부모 ID
Optional<String> parentId = identifier.getParentId();
```

### 2.5 TestExecutionListener

테스트 실행 이벤트를 수신합니다.

```java
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.engine.TestExecutionResult;

public class MyListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // 전체 실행 시작
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        // 전체 실행 종료
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        // 개별 테스트/컨테이너 시작
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
                                  TestExecutionResult result) {
        // 개별 테스트/컨테이너 종료
        switch (result.getStatus()) {
            case SUCCESSFUL -> handleSuccess();
            case FAILED -> handleFailure(result.getThrowable());
            case ABORTED -> handleAborted();
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        // 테스트 건너뜀
    }
}
```

---

## 3. 프로젝트에서의 활용

> **Note**: 아래 코드들은 `autotest-runner` 모듈에 위치합니다.

### 3.1 테스트 발견 (TestRunner.java)

```java
public class TestRunner {

    public static void main(String[] args) {
        if ("discover".equals(args[0])) {
            runDiscover(args[1]); // 패키지명
        } else if ("run".equals(args[0])) {
            runTests(Arrays.copyOfRange(args, 1, args.length)); // 클래스명들
        }
    }

    private static void runDiscover(String rootPackage) {
        // 1. Launcher 생성
        Launcher launcher = LauncherFactory.create();

        // 2. 발견 요청 생성
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectPackage(rootPackage))
            .build();

        // 3. 테스트 발견
        TestPlan testPlan = launcher.discover(request);

        // 4. 결과 변환
        List<TestNodeDto> nodes = new ArrayList<>();
        Set<TestIdentifier> roots = testPlan.getRoots();

        for (TestIdentifier root : roots) {
            collectNodes(testPlan, root, nodes);
        }

        // 5. JSON 출력
        System.out.println("###TEST_RUNNER_RESULT###");
        System.out.println(toJson(new DiscoverResult(true, null, nodes)));
    }

    private static void collectNodes(TestPlan testPlan,
                                     TestIdentifier current,
                                     List<TestNodeDto> nodes) {
        // 현재 노드 추가
        nodes.add(new TestNodeDto(
            current.getUniqueId(),
            current.getParentId().orElse(null),
            current.getDisplayName(),
            extractClassName(current),
            current.isContainer() ? "CONTAINER" : "TEST"
        ));

        // 자식 노드들 재귀 처리
        for (TestIdentifier child : testPlan.getChildren(current)) {
            collectNodes(testPlan, child, nodes);
        }
    }
}
```

### 3.2 테스트 실행 (TestRunner.java)

```java
private static void runTests(String[] classNames) {
    // 1. Launcher 생성
    Launcher launcher = LauncherFactory.create();

    // 2. Listener 등록
    TestRunnerListener listener = new TestRunnerListener();
    launcher.registerTestExecutionListeners(listener);

    // 3. 실행 요청 생성
    List<DiscoverySelector> selectors = Arrays.stream(classNames)
        .map(DiscoverySelectors::selectClass)
        .collect(Collectors.toList());

    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectors)
        .build();

    // 4. 테스트 실행
    launcher.execute(request);

    // 5. 결과 수집 및 출력
    TestSummaryDto summary = listener.buildSummary();
    List<TestResultDto> results = listener.findRootResults();

    System.out.println("###TEST_RUNNER_RESULT###");
    System.out.println(toJson(new RunResult(true, null, summary, results)));
}
```

### 3.3 실행 이벤트 처리 (TestRunnerListener.java)

```java
public class TestRunnerListener implements TestExecutionListener {

    private final Map<String, TestResultDto> nodeMap = new ConcurrentHashMap<>();
    private final Map<String, Long> startTimeMap = new ConcurrentHashMap<>();
    private final Map<String, ByteArrayOutputStream> stdoutMap = new ConcurrentHashMap<>();

    // 원본 stdout 저장
    private final PrintStream originalOut = System.out;

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        String id = testIdentifier.getUniqueId();

        // 시작 시간 기록
        startTimeMap.put(id, System.currentTimeMillis());

        // 노드 생성
        TestResultDto node = new TestResultDto(
            id,
            testIdentifier.getDisplayName(),
            "RUNNING",
            0L,
            null,
            null,
            null,
            new ArrayList<>()
        );
        nodeMap.put(id, node);

        // 부모에 자식으로 추가
        testIdentifier.getParentId().ifPresent(parentId -> {
            TestResultDto parent = nodeMap.get(parentId);
            if (parent != null) {
                parent.children().add(node);
            }
        });

        // stdout 캡처 시작
        if (testIdentifier.isTest()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stdoutMap.put(id, baos);

            // TeeOutputStream으로 stdout 가로채기
            System.setOut(new PrintStream(new TeeOutputStream(originalOut, baos)));
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
                                  TestExecutionResult result) {
        String id = testIdentifier.getUniqueId();
        TestResultDto node = nodeMap.get(id);

        if (node != null) {
            // 실행 시간 계산
            long duration = System.currentTimeMillis() - startTimeMap.get(id);
            node.setDurationMillis(duration);

            // 상태 설정
            switch (result.getStatus()) {
                case SUCCESSFUL -> node.setStatus("SUCCESS");
                case FAILED -> {
                    node.setStatus("FAILED");
                    result.getThrowable().ifPresent(t -> {
                        node.setErrorMessage(t.getMessage());
                        node.setStackTrace(getStackTrace(t));
                    });
                }
                case ABORTED -> node.setStatus("SKIPPED");
            }

            // stdout 저장
            if (testIdentifier.isTest()) {
                ByteArrayOutputStream baos = stdoutMap.get(id);
                if (baos != null) {
                    node.setStdout(baos.toString());
                }
                // stdout 복원
                System.setOut(originalOut);
            }
        }
    }

    public TestSummaryDto buildSummary() {
        int total = 0, success = 0, failed = 0, skipped = 0;
        long totalDuration = 0;

        for (TestResultDto node : nodeMap.values()) {
            if (isTestNode(node)) {
                total++;
                switch (node.status()) {
                    case "SUCCESS" -> success++;
                    case "FAILED" -> failed++;
                    case "SKIPPED" -> skipped++;
                }
                totalDuration += node.durationMillis();
            }
        }

        return new TestSummaryDto(total, success, failed, skipped, totalDuration);
    }

    public List<TestResultDto> findRootResults() {
        // engine 노드의 직접 자식들 반환
        return nodeMap.values().stream()
            .filter(node -> node.id().startsWith("[engine:"))
            .filter(node -> !node.id().equals("[engine:junit-jupiter]"))
            .collect(Collectors.toList());
    }
}
```

---

## 4. UniqueId 구조

JUnit Platform은 모든 테스트를 UniqueId로 식별합니다.

### 4.1 UniqueId 형식

```
[engine:junit-jupiter]                              # 엔진 루트
/[class:com.example.MyTest]                         # 테스트 클래스
/[method:testMethod()]                              # 테스트 메서드

또는 (nested class인 경우)

[engine:junit-jupiter]
/[class:com.example.OuterTest]
/[nested-class:InnerTest]                           # 내부 클래스
/[method:testInnerMethod()]
```

### 4.2 프로젝트에서의 UniqueId 파싱 (SourceCodeService.java)

```java
public class SourceCodeService {

    private static final Pattern CLASS_PATTERN =
        Pattern.compile("\\[class:([^\\]]+)\\]");
    private static final Pattern NESTED_CLASS_PATTERN =
        Pattern.compile("\\[nested-class:([^\\]]+)\\]");
    private static final Pattern METHOD_PATTERN =
        Pattern.compile("\\[method:([^\\]]+)\\]");

    public ParsedUniqueId parseUniqueId(String uniqueId) {
        // 클래스명 추출
        Matcher classMatcher = CLASS_PATTERN.matcher(uniqueId);
        String className = classMatcher.find() ? classMatcher.group(1) : null;

        // nested class 추출
        List<String> nestedClasses = new ArrayList<>();
        Matcher nestedMatcher = NESTED_CLASS_PATTERN.matcher(uniqueId);
        while (nestedMatcher.find()) {
            nestedClasses.add(nestedMatcher.group(1));
        }

        // 메서드명 추출
        Matcher methodMatcher = METHOD_PATTERN.matcher(uniqueId);
        String methodName = methodMatcher.find() ? methodMatcher.group(1) : null;

        return new ParsedUniqueId(className, nestedClasses, methodName);
    }
}
```

---

## 5. Discovery Selectors

테스트를 선택하는 다양한 방법:

```java
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

// 패키지 선택
selectPackage("com.example.tests")

// 클래스 선택
selectClass("com.example.MyTest")
selectClass(MyTest.class)

// 메서드 선택
selectMethod("com.example.MyTest#testMethod")
selectMethod(MyTest.class, "testMethod")

// 디렉토리 선택
selectDirectory("/path/to/test/classes")

// 클래스패스 루트 선택
selectClasspathRoots(Set.of(Path.of("/path/to/classes")))

// 모듈 선택 (Java 9+)
selectModule("my.module")

// UniqueId로 선택
selectUniqueId("[engine:junit-jupiter]/[class:MyTest]/[method:test()]")
```

---

## 6. Filters

발견된 테스트를 필터링:

```java
import static org.junit.platform.engine.discovery.ClassNameFilter.*;
import static org.junit.platform.launcher.TagFilter.*;

LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
    .selectors(selectPackage("com.example"))
    .filters(
        // 클래스명 필터
        includeClassNamePatterns(".*Test"),
        excludeClassNamePatterns(".*IntegrationTest"),

        // 태그 필터
        includeTags("fast", "unit"),
        excludeTags("slow", "integration")
    )
    .build();
```

---

## 7. 의존성 설정

### autotest-runner/build.gradle.kts

```kotlin
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    // JUnit Platform Launcher API (필수)
    implementation("org.junit.platform:junit-platform-launcher:1.11.4")

    // JUnit Jupiter Engine (JUnit 5 테스트 실행)
    implementation("org.junit.jupiter:junit-jupiter-engine:5.11.4")

    // JSON 직렬화
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
}

// Shadow JAR로 모든 의존성 포함
tasks.shadowJar {
    archiveBaseName.set("autotest-runner")
    archiveClassifier.set("")
    mergeServiceFiles()  // JUnit 엔진 SPI 파일 병합 필수!
}
```

> **중요**: `mergeServiceFiles()`는 JUnit Platform이 엔진을 찾을 수 있도록
> `META-INF/services/` 파일들을 병합합니다.

### Maven (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.platform</groupId>
        <artifactId>junit-platform-launcher</artifactId>
        <version>1.11.4</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-engine</artifactId>
        <version>5.11.4</version>
    </dependency>
</dependencies>
```

---

## 8. 실행 흐름 요약

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         JUnit Platform 실행 흐름                             │
│                                                                              │
│  ┌─────────────────┐                                                        │
│  │ LauncherFactory │                                                        │
│  │    .create()    │                                                        │
│  └────────┬────────┘                                                        │
│           │                                                                  │
│           ▼                                                                  │
│  ┌─────────────────┐                                                        │
│  │    Launcher     │ ◀─── registerTestExecutionListeners(listener)          │
│  └────────┬────────┘                                                        │
│           │                                                                  │
│           │  discover(request) 또는 execute(request)                        │
│           ▼                                                                  │
│  ┌─────────────────┐      ┌─────────────────┐                               │
│  │Discovery Request│      │  Test Engines   │                               │
│  │                 │─────▶│                 │                               │
│  │ - Selectors     │      │ - JUnit Jupiter │                               │
│  │ - Filters       │      │ - JUnit Vintage │                               │
│  └─────────────────┘      └────────┬────────┘                               │
│                                    │                                         │
│                                    ▼                                         │
│                           ┌─────────────────┐                               │
│                           │    TestPlan     │                               │
│                           │                 │                               │
│                           │ TestIdentifier  │                               │
│                           │ TestIdentifier  │                               │
│                           │ TestIdentifier  │                               │
│                           └────────┬────────┘                               │
│                                    │ execute()                               │
│                                    ▼                                         │
│                           ┌─────────────────┐                               │
│                           │ Listener 콜백   │                               │
│                           │                 │                               │
│                           │ executionStarted│                               │
│                           │ executionFinished│                              │
│                           │ executionSkipped│                               │
│                           └─────────────────┘                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. 참고 자료

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [JUnit Platform Launcher API](https://junit.org/junit5/docs/current/user-guide/#launcher-api)
- [TestExecutionListener Javadoc](https://junit.org/junit5/docs/current/api/org.junit.platform.launcher/org/junit/platform/launcher/TestExecutionListener.html)
