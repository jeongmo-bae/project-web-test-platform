# Hot Reload 메커니즘

이 문서는 Web Test Platform에서 테스트 코드 수정 시 애플리케이션 재시작 없이 즉시 반영되는 Hot Reload 기능이 어떻게 구현되었는지 설명합니다.

---

## 1. 문제 정의

### 1.1 일반적인 Java 애플리케이션의 클래스 로딩

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        일반적인 클래스 로딩 문제                              │
│                                                                              │
│  Spring Boot App 시작                                                        │
│       │                                                                      │
│       ▼                                                                      │
│  ClassLoader가 클래스 로드                                                   │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         JVM 메모리                                   │   │
│  │                                                                      │   │
│  │   MyTest.class (v1)  ◀── 한 번 로드되면 교체 불가                    │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  테스트 코드 수정 (v2)                                                       │
│       │                                                                      │
│       ▼                                                                      │
│  JVM에는 여전히 v1이 캐시됨 ❌                                               │
│       │                                                                      │
│       ▼                                                                      │
│  v2 반영하려면 애플리케이션 재시작 필요                                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 왜 이런 문제가 발생하는가?

1. **ClassLoader 캐싱**: Java ClassLoader는 한 번 로드한 클래스를 캐시합니다.
2. **클래스 교체 불가**: 동일한 ClassLoader에서 같은 클래스를 다시 로드할 수 없습니다.
3. **JVM 제약**: 표준 JVM에서는 실행 중인 클래스를 교체하는 것이 제한됩니다.

---

## 2. 해결책: 별도 JVM 프로세스

### 2.1 핵심 아이디어

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           해결책: 별도 JVM                                   │
│                                                                              │
│  테스트 실행 요청                                                            │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Spring Boot Application                          │   │
│  │                        (계속 실행 중)                                 │   │
│  │                                                                      │   │
│  │   1. gradle compileJava 실행 (최신 코드 컴파일)                      │   │
│  │   2. ProcessBuilder로 새 JVM 프로세스 시작                           │   │
│  │   3. 결과 수신                                                       │   │
│  │                                                                      │   │
│  └────────────────────────────┬────────────────────────────────────────┘   │
│                               │ Process 실행                                │
│                               ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      새 JVM 프로세스                                  │   │
│  │                                                                      │   │
│  │   ┌─────────────────────────────────────────────────────────────┐   │   │
│  │   │                    새 ClassLoader                            │   │   │
│  │   │                                                              │   │   │
│  │   │   MyTest.class (v2) ◀── 항상 최신 버전 로드! ✅              │   │   │
│  │   │                                                              │   │   │
│  │   └─────────────────────────────────────────────────────────────┘   │   │
│  │                                                                      │   │
│  │   테스트 실행 → 결과 JSON 출력 → 프로세스 종료                       │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 왜 이 방법이 효과적인가?

1. **새 JVM = 새 ClassLoader**: 매번 새로운 JVM을 시작하면 클래스가 처음부터 로드됩니다.
2. **최신 바이트코드 로드**: 컴파일 후 시작하므로 항상 최신 `.class` 파일을 읽습니다.
3. **격리된 환경**: 테스트 코드가 메인 애플리케이션에 영향을 주지 않습니다.
4. **의존성 분리**: 테스트 코드의 의존성이 메인 앱과 충돌하지 않습니다.

---

## 3. 구현 상세

### 3.1 ProcessExecutorService.java (autotest-app 모듈)

```java
@Service
public class ProcessExecutorService {

    @Value("${testcode.project-path}")
    private String testcodeProjectPath;

    // autotest-runner.jar 경로
    private String runnerJarPath;

    /**
     * 테스트 코드 컴파일
     * - Gradle wrapper를 사용하여 테스트 프로젝트 컴파일
     */
    public void compileTestCode() throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(testcodeProjectPath));

        // OS에 따른 Gradle 명령어 선택
        String gradleCommand = isWindows() ? "gradlew.bat" : "./gradlew";
        pb.command(gradleCommand, "compileJava", "--quiet");

        // 환경 변수 상속
        pb.inheritIO();

        Process process = pb.start();

        // 타임아웃 설정 (5분)
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Compilation timed out");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("Compilation failed with exit code: "
                + process.exitValue());
        }
    }

    /**
     * 테스트 발견
     * - 별도 JVM에서 TestRunner.main("discover", packageName) 실행
     */
    public TestRunner.DiscoverResult discoverTests(String rootPackage)
            throws Exception {
        List<String> command = buildJavaCommand("discover", rootPackage);
        String output = executeProcess(command);
        return parseResult(output, TestRunner.DiscoverResult.class);
    }

    /**
     * 테스트 실행
     * - 별도 JVM에서 TestRunner.main("run", class1, class2, ...) 실행
     */
    public TestRunner.RunResult runTests(List<String> classNames)
            throws Exception {
        String[] args = new String[classNames.size() + 1];
        args[0] = "run";
        for (int i = 0; i < classNames.size(); i++) {
            args[i + 1] = classNames.get(i);
        }

        List<String> command = buildJavaCommand(args);
        String output = executeProcess(command);
        return parseResult(output, TestRunner.RunResult.class);
    }

    /**
     * Java 실행 명령어 구성
     */
    private List<String> buildJavaCommand(String... args) throws Exception {
        List<String> command = new ArrayList<>();

        // Java 실행 파일 경로
        String javaHome = System.getProperty("java.home");
        String java = javaHome + File.separator + "bin" + File.separator + "java";
        command.add(java);

        // Classpath 구성
        command.add("-cp");
        command.add(buildClasspath());

        // 메인 클래스
        command.add("testauto.runner.TestRunner");

        // 인자들
        command.addAll(Arrays.asList(args));

        return command;
    }

    /**
     * Classpath 구성
     * - 현재 앱의 classpath
     * - 테스트 프로젝트의 컴파일된 클래스
     * - 테스트 프로젝트의 의존성
     */
    private String buildClasspath() throws Exception {
        StringBuilder cp = new StringBuilder();
        String separator = File.pathSeparator; // ":" (Unix) 또는 ";" (Windows)

        // 1. 현재 애플리케이션의 classpath
        String currentCp = System.getProperty("java.class.path");
        cp.append(currentCp);

        // 2. 테스트 프로젝트의 컴파일된 클래스
        Path classesDir = Paths.get(testcodeProjectPath,
            "build", "classes", "java", "main");
        if (Files.exists(classesDir)) {
            cp.append(separator).append(classesDir);
        }

        // 3. 테스트 프로젝트의 의존성 JAR들
        Path libsDir = Paths.get(testcodeProjectPath, "build", "libs");
        if (Files.exists(libsDir)) {
            try (DirectoryStream<Path> stream =
                    Files.newDirectoryStream(libsDir, "*.jar")) {
                for (Path jar : stream) {
                    cp.append(separator).append(jar);
                }
            }
        }

        // 4. 테스트 프로젝트의 Gradle 의존성
        Path depsDir = Paths.get(testcodeProjectPath, "build", "dependencies");
        if (Files.exists(depsDir)) {
            Files.walk(depsDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .forEach(jar -> cp.append(separator).append(jar));
        }

        return cp.toString();
    }

    /**
     * 프로세스 실행 및 출력 수집
     */
    private String executeProcess(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // stderr를 stdout으로 합침

        Process process = pb.start();

        // 출력 수집
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // 프로세스 완료 대기
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Process timed out");
        }

        return output.toString();
    }

    /**
     * 결과 파싱
     * - ###TEST_RUNNER_RESULT### 마커 이후의 JSON 추출
     */
    private <T> T parseResult(String output, Class<T> resultClass)
            throws Exception {
        String marker = "###TEST_RUNNER_RESULT###";
        int markerIndex = output.indexOf(marker);

        if (markerIndex == -1) {
            throw new RuntimeException("Result marker not found in output:\n"
                + output);
        }

        String json = output.substring(markerIndex + marker.length()).trim();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, resultClass);
    }
}
```

### 3.2 TestRunner.java (autotest-runner 모듈, 별도 JVM에서 실행)

```java
public class TestRunner {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            String command = args[0];

            if ("discover".equals(command)) {
                runDiscover(args[1]);
            } else if ("run".equals(command)) {
                runTests(Arrays.copyOfRange(args, 1, args.length));
            }
        } catch (Exception e) {
            // 에러도 JSON으로 출력
            System.out.println("###TEST_RUNNER_RESULT###");
            System.out.println(toErrorJson(e));
        }
    }

    private static void runDiscover(String rootPackage) {
        Launcher launcher = LauncherFactory.create();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectPackage(rootPackage))
            .build();

        TestPlan testPlan = launcher.discover(request);

        List<TestNodeDto> nodes = collectNodes(testPlan);

        // 결과 출력 (마커 사용)
        System.out.println("###TEST_RUNNER_RESULT###");
        System.out.println(toJson(new DiscoverResult(true, null, nodes)));
    }

    private static void runTests(String[] classNames) {
        Launcher launcher = LauncherFactory.create();

        // 리스너 등록
        TestRunnerListener listener = new TestRunnerListener();
        launcher.registerTestExecutionListeners(listener);

        // 클래스 선택
        List<DiscoverySelector> selectors = Arrays.stream(classNames)
            .map(DiscoverySelectors::selectClass)
            .collect(Collectors.toList());

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectors)
            .build();

        // 실행
        launcher.execute(request);

        // 결과 수집
        TestSummaryDto summary = listener.buildSummary();
        List<TestResultDto> results = listener.findRootResults();

        // 결과 출력 (마커 사용)
        System.out.println("###TEST_RUNNER_RESULT###");
        System.out.println(toJson(new RunResult(true, null, summary, results)));
    }

    // DTO 정의
    public record DiscoverResult(
        boolean success,
        String error,
        List<TestNodeDto> nodes
    ) {}

    public record RunResult(
        boolean success,
        String error,
        TestSummaryDto summary,
        List<TestResultDto> results
    ) {}
}
```

---

## 4. 결과 전달 메커니즘

### 4.1 마커 기반 JSON 전달

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           결과 전달 프로토콜                                  │
│                                                                              │
│  TestRunner (별도 JVM)                                                       │
│       │                                                                      │
│       │ stdout                                                               │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                      │   │
│  │  [테스트 실행 중 발생하는 일반 출력]                                  │   │
│  │  Running test: MyTest                                                │   │
│  │  Test passed: testMethod1                                            │   │
│  │  Test failed: testMethod2                                            │   │
│  │  ...                                                                 │   │
│  │                                                                      │   │
│  │  ###TEST_RUNNER_RESULT###       ◀── 마커 (이 이후가 JSON)            │   │
│  │  {"success":true,"summary":{...},"results":[...]}                    │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ProcessExecutorService.parseResult()                                        │
│       │                                                                      │
│       │ 마커 이후 JSON 추출                                                  │
│       ▼                                                                      │
│  RunResult 객체로 역직렬화                                                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 왜 마커를 사용하는가?

1. **stdout 오염 방지**: 테스트 중 `System.out.println()`이 호출될 수 있음
2. **결과 분리**: 일반 출력과 결과 JSON을 명확히 분리
3. **파싱 안정성**: 마커 위치를 찾아 정확한 JSON 영역만 파싱

---

## 5. stdout 캡처

### 5.1 TeeOutputStream

테스트 실행 중 stdout을 캡처하면서도 원래 출력을 유지합니다.

```java
public class TestRunnerListener implements TestExecutionListener {

    private final PrintStream originalOut = System.out;
    private final Map<String, ByteArrayOutputStream> stdoutMap =
        new ConcurrentHashMap<>();

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            // 새 버퍼 생성
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            stdoutMap.put(testIdentifier.getUniqueId(), buffer);

            // TeeOutputStream으로 stdout 교체
            TeeOutputStream tee = new TeeOutputStream(originalOut, buffer);
            System.setOut(new PrintStream(tee));
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
                                  TestExecutionResult result) {
        if (testIdentifier.isTest()) {
            // 캡처된 stdout 저장
            ByteArrayOutputStream buffer =
                stdoutMap.get(testIdentifier.getUniqueId());
            String stdout = buffer != null ? buffer.toString() : null;

            // 결과에 저장
            TestResultDto node = nodeMap.get(testIdentifier.getUniqueId());
            node.setStdout(stdout);

            // 원래 stdout 복원
            System.setOut(originalOut);
        }
    }

    /**
     * 두 개의 OutputStream에 동시에 쓰는 스트림
     */
    private static class TeeOutputStream extends OutputStream {
        private final OutputStream out1; // 원래 stdout (화면)
        private final OutputStream out2; // 캡처 버퍼

        public TeeOutputStream(OutputStream out1, OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(int b) throws IOException {
            out1.write(b); // 화면에 출력
            out2.write(b); // 버퍼에 저장
        }

        @Override
        public void flush() throws IOException {
            out1.flush();
            out2.flush();
        }

        @Override
        public void close() throws IOException {
            out1.close();
            out2.close();
        }
    }
}
```

---

## 6. 전체 실행 흐름

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Hot Reload 전체 흐름                               │
│                                                                              │
│  1. 사용자가 테스트 코드 수정                                                │
│       │                                                                      │
│       ▼                                                                      │
│  2. 웹 UI에서 "새로고침" 또는 "실행" 버튼 클릭                               │
│       │                                                                      │
│       ▼                                                                      │
│  3. POST /api/tests/refresh 또는 /api/tests/run                             │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  4. ProcessExecutorService.compileTestCode()                         │   │
│  │                                                                      │   │
│  │     ProcessBuilder pb = new ProcessBuilder("./gradlew", "compileJava");
│  │     pb.directory(testcodeProjectPath);                               │   │
│  │     pb.start().waitFor();                                            │   │
│  │                                                                      │   │
│  │     → 최신 .java → 최신 .class 생성                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  5. ProcessExecutorService.discoverTests() 또는 .runTests()          │   │
│  │                                                                      │   │
│  │     List<String> command = [                                         │   │
│  │         "java",                                                      │   │
│  │         "-cp", buildClasspath(),  // 최신 클래스 포함                 │   │
│  │         "testauto.runner.TestRunner",                                │   │
│  │         "run",                                                       │   │
│  │         "com.example.MyTest"                                         │   │
│  │     ];                                                               │   │
│  │                                                                      │   │
│  │     ProcessBuilder pb = new ProcessBuilder(command);                 │   │
│  │     Process process = pb.start();  // 새 JVM 시작!                   │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  6. 새 JVM에서 TestRunner.main() 실행                                │   │
│  │                                                                      │   │
│  │     - 새 ClassLoader가 최신 .class 파일 로드                         │   │
│  │     - Launcher.execute() 실행                                        │   │
│  │     - TestRunnerListener가 결과 수집                                 │   │
│  │     - System.out.println("###TEST_RUNNER_RESULT###");                │   │
│  │     - System.out.println(jsonResult);                                │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  7. 결과 파싱 및 DB 저장                                             │   │
│  │                                                                      │   │
│  │     String output = process.getInputStream().read();                 │   │
│  │     RunResult result = parseResult(output);                          │   │
│  │     executionRepository.saveAllResults(result);                      │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│  8. 웹 UI에서 결과 조회                                                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. 대안 비교

| 방법 | 장점 | 단점 |
|------|------|------|
| **별도 JVM (현재 방식)** | 항상 최신 클래스 로드, 격리된 환경 | 프로세스 시작 오버헤드 |
| **커스텀 ClassLoader** | 오버헤드 적음 | 구현 복잡, 클래스 언로딩 문제 |
| **Spring Boot DevTools** | 자동 재시작 | 전체 앱 재시작 필요, 개발 환경 전용 |
| **JRebel (상용)** | 즉시 반영, 오버헤드 적음 | 비용, 라이선스 |
| **DCEVM/HotswapAgent** | 무료, 효과적 | JVM 패치 필요, 호환성 문제 |

**별도 JVM 방식을 선택한 이유:**
- 가장 안정적이고 신뢰할 수 있음
- 추가 도구나 라이선스 불필요
- 운영 환경에서도 동일하게 동작
- 프로세스 시작 오버헤드는 테스트 실행 시간에 비해 무시할 수준

---

## 8. 성능 최적화

### 8.1 컴파일 최적화

```java
// --quiet 옵션으로 불필요한 출력 제거
pb.command(gradleCommand, "compileJava", "--quiet");

// 점진적 빌드 활용 (Gradle이 자동으로 변경된 파일만 컴파일)
```

### 8.2 프로세스 재사용 (미래 개선 가능)

```java
// 현재: 매번 새 프로세스 시작
Process process = pb.start();
process.waitFor();

// 미래 개선: 프로세스 풀 또는 데몬 프로세스
// - 테스트 간 JVM 재사용
// - 클래스 로딩 오버헤드만 발생
```

### 8.3 병렬 실행

```java
// 여러 테스트 클래스를 한 번에 실행
runTests(List.of("Class1", "Class2", "Class3"));
// → 하나의 JVM에서 모든 클래스 실행
```

---

## 9. 주의사항

### 9.1 클래스패스 관리

테스트 프로젝트의 모든 의존성이 클래스패스에 포함되어야 합니다:

```java
private String buildClasspath() {
    // 1. 현재 앱 classpath
    // 2. 테스트 프로젝트 build/classes/java/main
    // 3. 테스트 프로젝트 build/libs/*.jar
    // 4. 테스트 프로젝트 build/dependencies/**/*.jar
}
```

### 9.2 타임아웃 설정

무한 루프 등을 방지하기 위한 타임아웃:

```java
boolean finished = process.waitFor(10, TimeUnit.MINUTES);
if (!finished) {
    process.destroyForcibly();
    throw new RuntimeException("Test execution timed out");
}
```

### 9.3 에러 처리

컴파일 에러나 런타임 에러를 적절히 처리:

```java
try {
    compileTestCode();
    runTests(classNames);
} catch (Exception e) {
    execution.setStatus("FAILED");
    execution.setErrorMessage(e.getMessage());
    executionRepository.updateExecution(execution);
}
```
