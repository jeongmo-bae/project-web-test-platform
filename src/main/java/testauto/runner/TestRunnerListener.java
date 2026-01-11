package testauto.runner;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import testauto.runner.TestRunner.TestResultDto;
import testauto.runner.TestRunner.TestSummaryDto;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트 실행 리스너 - 별도 JVM용 (JSON 직렬화 가능)
 */
public class TestRunnerListener implements TestExecutionListener {

    private final Map<String, MutableTestResult> nodeMap = new ConcurrentHashMap<>();
    private final List<MutableTestResult> roots = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Long> startTimeMap = new ConcurrentHashMap<>();

    private final PrintStream originalOut = System.out;
    private final Map<String, ByteArrayOutputStream> stdoutCaptures = new ConcurrentHashMap<>();

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (!testIdentifier.isTest() && !testIdentifier.isContainer()) {
            return;
        }

        String id = testIdentifier.getUniqueId();
        String displayName = testIdentifier.getDisplayName();
        String parentId = testIdentifier.getParentId().orElse(null);

        MutableTestResult node = new MutableTestResult(id, displayName);
        nodeMap.put(id, node);

        if (parentId != null) {
            MutableTestResult parent = nodeMap.get(parentId);
            if (parent != null) {
                parent.children.add(node);
            } else {
                roots.add(node);
            }
        } else {
            roots.add(node);
        }

        startTimeMap.put(id, System.currentTimeMillis());

        // Start capturing stdout for actual test methods only
        if (testIdentifier.isTest()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stdoutCaptures.put(id, baos);

            PrintStream teeStream = new PrintStream(new TeeOutputStream(originalOut, baos), true);
            System.setOut(teeStream);
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        if (!testIdentifier.isTest() && !testIdentifier.isContainer()) {
            return;
        }

        String status = switch (result.getStatus()) {
            case SUCCESSFUL -> "SUCCESS";
            case FAILED -> "FAILED";
            case ABORTED -> "SKIPPED";
        };

        String errorMessage = null;
        String stackTrace = null;

        if (result.getThrowable().isPresent()) {
            Throwable t = result.getThrowable().get();
            errorMessage = t.getMessage();
            stackTrace = getStackTraceAsString(t);
        }

        String id = testIdentifier.getUniqueId();

        // Capture stdout if this is a test method
        String capturedStdout = null;
        if (testIdentifier.isTest()) {
            System.setOut(originalOut);

            ByteArrayOutputStream baos = stdoutCaptures.remove(id);
            if (baos != null && baos.size() > 0) {
                capturedStdout = baos.toString();
            }
        }

        MutableTestResult node = nodeMap.get(id);
        if (node != null) {
            node.status = status;

            Long startTime = startTimeMap.get(id);
            if (startTime != null) {
                node.durationMillis = System.currentTimeMillis() - startTime;
            }

            node.errorMessage = errorMessage;
            node.stackTrace = stackTrace;
            node.stdout = capturedStdout;
        }
    }

    public List<TestResultDto> findRootResults() {
        return roots.stream()
                .map(this::toDto)
                .toList();
    }

    public TestSummaryDto buildSummary() {
        int total = 0, success = 0, failed = 0, skipped = 0;
        long totalDuration = 0;

        for (MutableTestResult root : roots) {
            int[] counts = countResults(root);
            total += counts[0];
            success += counts[1];
            failed += counts[2];
            skipped += counts[3];
            totalDuration += counts[4];
        }

        return new TestSummaryDto(total, success, failed, skipped, totalDuration);
    }

    private int[] countResults(MutableTestResult node) {
        int total = 0, success = 0, failed = 0, skipped = 0;
        long duration = 0;

        if (node.children.isEmpty()) {
            total = 1;
            duration = node.durationMillis;
            switch (node.status) {
                case "SUCCESS" -> success = 1;
                case "FAILED" -> failed = 1;
                case "SKIPPED" -> skipped = 1;
            }
        }

        for (MutableTestResult child : node.children) {
            int[] childCounts = countResults(child);
            total += childCounts[0];
            success += childCounts[1];
            failed += childCounts[2];
            skipped += childCounts[3];
            duration += childCounts[4];
        }

        return new int[]{total, success, failed, skipped, (int) duration};
    }

    private TestResultDto toDto(MutableTestResult node) {
        List<TestResultDto> childDtos = node.children.stream()
                .map(this::toDto)
                .toList();

        return new TestResultDto(
                node.id,
                node.displayName,
                node.status,
                node.durationMillis,
                node.errorMessage,
                node.stackTrace,
                node.stdout,
                childDtos
        );
    }

    private String getStackTraceAsString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement element : t.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }

    // Mutable class for building results
    private static class MutableTestResult {
        final String id;
        final String displayName;
        String status = "RUNNING";
        long durationMillis;
        String errorMessage;
        String stackTrace;
        String stdout;
        final List<MutableTestResult> children = Collections.synchronizedList(new ArrayList<>());

        MutableTestResult(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    // TeeOutputStream writes to two output streams
    private static class TeeOutputStream extends OutputStream {
        private final OutputStream out1;
        private final OutputStream out2;

        TeeOutputStream(OutputStream out1, OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            out1.write(b);
            out2.write(b);
        }

        @Override
        public void write(byte[] b) throws java.io.IOException {
            out1.write(b);
            out2.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            out1.write(b, off, len);
            out2.write(b, off, len);
        }

        @Override
        public void flush() throws java.io.IOException {
            out1.flush();
            out2.flush();
        }

        @Override
        public void close() throws java.io.IOException {
            try {
                out1.close();
            } finally {
                out2.close();
            }
        }
    }
}
