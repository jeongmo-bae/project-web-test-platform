package testauto.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.*;

/**
 * 별도 JVM에서 실행되는 테스트 러너
 *
 * 사용법:
 *   java -cp <classpath> testauto.runner.TestRunner discover <rootPackage>
 *   java -cp <classpath> testauto.runner.TestRunner run <className1> [className2] ...
 */
public class TestRunner {

    private static final String RESULT_MARKER = "###TEST_RUNNER_RESULT###";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        if (args.length < 2) {
            printError("Usage: TestRunner <discover|run> <package|classNames...>");
            System.exit(1);
        }

        String mode = args[0];

        try {
            switch (mode) {
                case "discover" -> {
                    String rootPackage = args[1];
                    runDiscover(rootPackage);
                }
                case "run" -> {
                    List<String> classNames = Arrays.asList(args).subList(1, args.length);
                    runTests(classNames);
                }
                default -> {
                    printError("Unknown mode: " + mode);
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            printError("Execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 테스트 발견 모드
     */
    private static void runDiscover(String rootPackage) throws Exception {
        Launcher launcher = LauncherFactory.create();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage(rootPackage))
                .build();

        TestPlan testPlan = launcher.discover(request);

        List<TestNodeDto> nodes = new ArrayList<>();
        for (TestIdentifier root : testPlan.getRoots()) {
            collectNodes(testPlan, root, nodes);
        }

        DiscoverResult result = new DiscoverResult(true, null, nodes);
        printResult(result);
    }

    /**
     * 테스트 실행 모드
     */
    private static void runTests(List<String> classNames) throws Exception {
        TestRunnerListener listener = new TestRunnerListener();

        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);

        List<DiscoverySelector> selectors = classNames.stream()
                .<DiscoverySelector>map(DiscoverySelectors::selectClass)
                .toList();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors)
                .build();

        launcher.execute(request);

        RunResult result = new RunResult(
                true,
                null,
                listener.buildSummary(),
                listener.findRootResults()
        );
        printResult(result);
    }

    private static void collectNodes(TestPlan testPlan, TestIdentifier testIdentifier, List<TestNodeDto> nodes) {
        String uniqueId = testIdentifier.getUniqueId();
        String displayName = testIdentifier.getDisplayName();
        String parentId = testIdentifier.getParentId().orElse(null);

        TestSource source = testIdentifier.getSource().orElse(null);
        String className = null;
        if (source instanceof ClassSource classSource) {
            className = classSource.getClassName();
        }

        String nodeType = testIdentifier.isContainer() ? "CONTAINER" : "TEST";

        // 엔진 루트는 제외
        if (!uniqueId.contains("[engine:")) {
            nodes.add(new TestNodeDto(uniqueId, parentId, displayName, className, nodeType));
        } else if (uniqueId.equals("[engine:junit-jupiter]")) {
            // JUnit Jupiter 엔진 루트도 제외하지만 자식은 포함
        }

        for (TestIdentifier child : testPlan.getChildren(testIdentifier)) {
            collectNodes(testPlan, child, nodes);
        }
    }

    private static void printResult(Object result) throws Exception {
        String json = objectMapper.writeValueAsString(result);
        System.out.println(RESULT_MARKER);
        System.out.println(json);
    }

    private static void printError(String message) {
        try {
            ErrorResult result = new ErrorResult(false, message);
            String json = objectMapper.writeValueAsString(result);
            System.out.println(RESULT_MARKER);
            System.out.println(json);
        } catch (Exception e) {
            System.err.println("Failed to print error: " + message);
        }
    }

    // DTOs for JSON serialization
    public record TestNodeDto(
            String uniqueId,
            String parentUniqueId,
            String displayName,
            String className,
            String type
    ) {}

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

    public record ErrorResult(
            boolean success,
            String error
    ) {}

    public record TestSummaryDto(
            int total,
            int success,
            int failed,
            int skipped,
            long totalDurationMillis
    ) {}

    public record TestResultDto(
            String id,
            String displayName,
            String status,
            long durationMillis,
            String errorMessage,
            String stackTrace,
            String stdout,
            List<TestResultDto> children
    ) {}
}
