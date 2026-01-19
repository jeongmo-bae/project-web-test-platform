package testauto.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import testauto.runner.TestRunner;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProcessExecutorService {

    private static final String RESULT_MARKER = "###TEST_RUNNER_RESULT###";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${testcode.project-path}")
    private String testcodeProjectPath;

    // 애플리케이션 시작 시점의 JAVA_HOME을 캡처
    private String capturedJavaHome;

    // Spring Boot JAR에서 추출한 클래스패스 경로
    private Path extractedClassesPath;
    private Path extractedLibPath;

    @PostConstruct
    public void init() {
        // 1순위: 환경변수 JAVA_HOME
        capturedJavaHome = System.getenv("JAVA_HOME");

        // 2순위: 현재 JVM의 java.home 시스템 프로퍼티
        if (capturedJavaHome == null || capturedJavaHome.isBlank()) {
            capturedJavaHome = System.getProperty("java.home");
            log.info("JAVA_HOME 환경변수가 없어서 java.home 시스템 프로퍼티 사용: {}", capturedJavaHome);
        } else {
            log.info("캡처된 JAVA_HOME: {}", capturedJavaHome);
        }

        // Spring Boot JAR에서 BOOT-INF 추출
        extractBootInfFromJar();
    }

    @PreDestroy
    public void cleanup() {
        // 추출된 임시 디렉토리 정리
        if (extractedClassesPath != null) {
            try {
                deleteDirectory(extractedClassesPath.getParent());
                log.info("Cleaned up extracted BOOT-INF directory");
            } catch (Exception e) {
                log.warn("Failed to cleanup extracted directory", e);
            }
        }
    }

    private void extractBootInfFromJar() {
        try {
            String classpath = System.getProperty("java.class.path");
            String sep = isWindows() ? ";" : ":";
            String[] entries = classpath.split(java.util.regex.Pattern.quote(sep));

            // Spring Boot JAR 찾기 (autotest*.jar)
            Path springBootJar = null;
            for (String entry : entries) {
                if (entry.contains("autotest") && entry.endsWith(".jar") && !entry.contains("-plain.jar")) {
                    springBootJar = Path.of(entry);
                    break;
                }
            }

            if (springBootJar == null || !Files.exists(springBootJar)) {
                log.info("Spring Boot JAR not found in classpath, using direct classpath");
                return;
            }

            // JAR 내부에 BOOT-INF가 있는지 확인
            try (JarFile jarFile = new JarFile(springBootJar.toFile())) {
                JarEntry bootInfEntry = jarFile.getJarEntry("BOOT-INF/classes/");
                if (bootInfEntry == null) {
                    log.info("No BOOT-INF in JAR, using direct classpath");
                    return;
                }
            }

            log.info("Found Spring Boot JAR: {}, extracting BOOT-INF...", springBootJar);

            // 임시 디렉토리 생성
            Path tempDir = Files.createTempDirectory("autotest-extracted-");
            extractedClassesPath = tempDir.resolve("classes");
            extractedLibPath = tempDir.resolve("lib");
            Files.createDirectories(extractedClassesPath);
            Files.createDirectories(extractedLibPath);

            // JAR에서 BOOT-INF 추출
            try (JarFile jarFile = new JarFile(springBootJar.toFile())) {
                Enumeration<JarEntry> entries2 = jarFile.entries();
                while (entries2.hasMoreElements()) {
                    JarEntry entry = entries2.nextElement();
                    String entryName = entry.getName();

                    if (entryName.startsWith("BOOT-INF/classes/")) {
                        String relativePath = entryName.substring("BOOT-INF/classes/".length());
                        if (!relativePath.isEmpty()) {
                            Path targetPath = extractedClassesPath.resolve(relativePath);
                            if (entry.isDirectory()) {
                                Files.createDirectories(targetPath);
                            } else {
                                Files.createDirectories(targetPath.getParent());
                                try (InputStream is = jarFile.getInputStream(entry)) {
                                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        }
                    } else if (entryName.startsWith("BOOT-INF/lib/") && entryName.endsWith(".jar")) {
                        String jarName = entryName.substring("BOOT-INF/lib/".length());
                        Path targetPath = extractedLibPath.resolve(jarName);
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }

            log.info("Extracted BOOT-INF/classes to: {}", extractedClassesPath);
            log.info("Extracted BOOT-INF/lib to: {}", extractedLibPath);

        } catch (Exception e) {
            log.warn("Failed to extract BOOT-INF from JAR, will use direct classpath", e);
        }
    }

    private void deleteDirectory(Path dir) throws Exception {
        if (dir != null && Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception e) {
                            // ignore
                        }
                    });
        }
    }

    /**
     * 테스트 코드 프로젝트 git pull
     */
    public void gitPull() throws Exception {
        log.info("Pulling latest code at: {}", testcodeProjectPath);

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(testcodeProjectPath));
        pb.command("git", "pull");
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("[git] {}", line);
            }
        }

        boolean finished = process.waitFor(2, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Git pull timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("Git pull returned non-zero exit code: {}, output: {}", exitCode, output);
            // git pull 실패해도 계속 진행 (네트워크 문제 등)
        } else {
            log.info("Git pull completed: {}", output.toString().trim());
        }
    }

    /**
     * Gradle로 테스트 코드 컴파일
     */
    public void compileTestCode() throws Exception {
        log.info("Compiling test code at: {}", testcodeProjectPath);

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(testcodeProjectPath));

        // 캡처된 JAVA_HOME 환경변수 설정
        setJavaHomeEnv(pb);

        // OS에 따라 gradle wrapper 또는 gradle 사용
        String gradleCommand = isWindows() ? "gradlew.bat" : "./gradlew";
        File gradleWrapper = new File(testcodeProjectPath, isWindows() ? "gradlew.bat" : "gradlew");
        String gradleJavaHomeProp = "-Dorg.gradle.java.home=" + capturedJavaHome;

        if (gradleWrapper.exists()) {
            pb.command(gradleCommand,
                    "--no-daemon",
                    gradleJavaHomeProp,
                    "compileJava",
                    "--quiet");
        } else {
            pb.command("gradle",
                    "--no-daemon",
                    gradleJavaHomeProp,
                    "compileJava",
                    "--quiet");
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 출력 로깅
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[gradle] {}", line);
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Gradle compile timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Gradle compile failed with exit code: " + exitCode);
        }

        log.info("Test code compilation completed");
    }

    /**
     * 테스트 발견 (별도 JVM)
     */
    public TestRunner.DiscoverResult discoverTests(String rootPackage) throws Exception {
        log.info("Discovering tests in package: {}", rootPackage);

        List<String> command = buildJavaCommand("discover", rootPackage);
        String output = executeProcess(command);

        return parseResult(output, TestRunner.DiscoverResult.class);
    }

    /**
     * 테스트 실행 (별도 JVM)
     */
    public TestRunner.RunResult runTests(List<String> classNames) throws Exception {
        log.info("Running tests: {}", classNames);

        List<String> args = new ArrayList<>();
        args.add("run");
        args.addAll(classNames);

        List<String> command = buildJavaCommand(args.toArray(new String[0]));
        String output = executeProcess(command);

        return parseResult(output, TestRunner.RunResult.class);
    }

    private List<String> buildJavaCommand(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(getJavaExecutable());
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.add("-cp");
        String classpath = buildClasspath();
        log.info("Built classpath: {}", classpath);
        command.add(classpath);
        command.add("testauto.runner.TestRunner");

        for (String arg : args) {
            command.add(arg);
        }

        return command;
    }

    private String buildClasspath() throws Exception {
        List<String> paths = new ArrayList<>();
        String sep = isWindows() ? ";" : ":";

        // 1. 현재 애플리케이션의 classpath (TestRunner 클래스 포함)
        if (extractedClassesPath != null && Files.exists(extractedClassesPath)) {
            // Spring Boot JAR에서 추출한 경로 사용
            paths.add(extractedClassesPath.toString());

            // 추출한 lib JAR들도 추가
            if (extractedLibPath != null && Files.exists(extractedLibPath)) {
                Files.list(extractedLibPath)
                        .filter(p -> p.toString().endsWith(".jar"))
                        .forEach(p -> paths.add(p.toString()));
            }
            log.info("Using extracted BOOT-INF paths for classpath");
        } else {
            // 직접 실행 모드 (IDE 또는 gradle bootRun)
            String currentClasspath = System.getProperty("java.class.path");
            Path baseDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            for (String entry : currentClasspath.split(java.util.regex.Pattern.quote(sep))) {
                if (entry == null || entry.isBlank()) continue;
                Path p = Path.of(entry);
                Path abs = p.isAbsolute() ? p : baseDir.resolve(p);
                paths.add(abs.toAbsolutePath().normalize().toString());
            }
            log.info("Using direct classpath (IDE/bootRun mode)");
        }

        // 2. 테스트 코드 프로젝트의 컴파일된 클래스
        Path testClassesPath = Path.of(testcodeProjectPath, "build", "classes", "java", "main");
        if (Files.exists(testClassesPath)) {
            paths.add(testClassesPath.toString());
        }

        // 3. 테스트 코드 프로젝트의 의존성 (libs 폴더가 있다면)
        Path libsPath = Path.of(testcodeProjectPath, "build", "libs");
        if (Files.exists(libsPath)) {
            Files.list(libsPath)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> paths.add(p.toString()));
        }

        // 4. 테스트 코드 프로젝트의 Gradle 캐시 의존성
        Path dependenciesPath = Path.of(testcodeProjectPath, "build", "dependencies");
        if (Files.exists(dependenciesPath)) {
            Files.walk(dependenciesPath)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> paths.add(p.toString()));
        }

        String separator = isWindows() ? ";" : ":";
        return paths.stream()
                .filter(p -> !p.isBlank())
                .collect(Collectors.joining(separator));
    }

    private String executeProcess(List<String> command) throws Exception {
        log.debug("Executing command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
//        pb.directory(new File(testcodeProjectPath));

        // 캡처된 JAVA_HOME 환경변수 설정
        setJavaHomeEnv(pb);

        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Test execution timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("Process exited with code {}, output: {}", exitCode, output);
        }
        log.info("Runner raw output:\n{}", output);
        return output.toString();
    }

    private <T> T parseResult(String output, Class<T> resultClass) throws Exception {
        // RESULT_MARKER 이후의 JSON 추출
        int markerIndex = output.indexOf(RESULT_MARKER);
        if (markerIndex == -1) {
            log.error("Result marker not found in output: {}", output);
            throw new RuntimeException("Failed to parse test result: marker not found");
        }

        String jsonPart = output.substring(markerIndex + RESULT_MARKER.length()).trim();

        // 여러 줄의 JSON을 하나로 합치기
        StringBuilder jsonBuilder = new StringBuilder();
        for (String line : jsonPart.split("\n")) {
            jsonBuilder.append(line);
        }

        return objectMapper.readValue(jsonBuilder.toString(), resultClass);
    }

    private String getJavaExecutable() {
        // 캡처된 JAVA_HOME 사용
        String javaBin = capturedJavaHome + File.separator + "bin" + File.separator + "java";
        if (isWindows()) {
            javaBin += ".exe";
        }
        return javaBin;
    }

    private void setJavaHomeEnv(ProcessBuilder pb) {
        if (capturedJavaHome != null && !capturedJavaHome.isBlank()) {
            Map<String, String> env = pb.environment();
            env.put("JAVA_HOME", capturedJavaHome);
            log.debug("ProcessBuilder JAVA_HOME 설정: {}", capturedJavaHome);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
