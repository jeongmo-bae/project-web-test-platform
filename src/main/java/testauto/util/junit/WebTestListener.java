package testauto.util.junit;

import testauto.domain.TestResult;
import testauto.domain.TestStatus;
import testauto.domain.TestSummary;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트 실행 리스너 - 각 실행마다 새 인스턴스 생성하여 사용
 */
public class WebTestListener implements TestExecutionListener {

    // id -> TestResult
    private final Map<String, TestResult> nodeMap = new ConcurrentHashMap<>();

    // 루트 노드들
    private final List<TestResult> roots = Collections.synchronizedList(new ArrayList<>());

    // 시작 시간 측정용
    private final Map<String, Long> startTimeMap = new ConcurrentHashMap<>();

    // Store original System.out
    private final PrintStream originalOut = System.out;

    // Map to store stdout captures for each test
    private final Map<String, ByteArrayOutputStream> stdoutCaptures = new ConcurrentHashMap<>();

    public void clear() {
        nodeMap.clear();
        roots.clear();
        startTimeMap.clear();
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (!testIdentifier.isTest() && !testIdentifier.isContainer()) {
            return;
        }

        String id = testIdentifier.getUniqueId();
        String displayName = testIdentifier.getDisplayName();
        String parentId = testIdentifier.getParentId().orElse(null);

        // 결과 노드 생성 및 저장
        TestResult node = new TestResult(id, displayName);
        nodeMap.put(id, node);

        if (parentId != null) {
            TestResult parent = nodeMap.get(parentId);
            if (parent != null) {
                parent.addChild(node);
            } else {
                roots.add(node);
            }
        } else {
            roots.add(node);
        }

        startTimeMap.put(id, System.currentTimeMillis());

        // Start capturing stdout for actual test methods only (not containers)
        if (testIdentifier.isTest()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stdoutCaptures.put(id, baos);

            // Create a TeeOutputStream that writes to both original output and capture buffer
            PrintStream teeStream = new PrintStream(new TeeOutputStream(originalOut, baos), true);
            System.setOut(teeStream);
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier,
                                  TestExecutionResult result) {

        if (!testIdentifier.isTest() && !testIdentifier.isContainer()) {
            return;
        }

        TestStatus status;
        switch (result.getStatus()) {
            case SUCCESSFUL -> status = TestStatus.SUCCESS;
            case FAILED     -> status = TestStatus.FAILED;
            case ABORTED    -> status = TestStatus.SKIPPED;
            default         -> status = TestStatus.SKIPPED;
        }

        final String[] errorMessage = {null};
        final String[] stackTrace = {null};

        result.getThrowable().ifPresent(t -> {
            errorMessage[0] = t.getMessage();
            stackTrace[0] = getStackTraceAsString(t);
        });

        String id = testIdentifier.getUniqueId();

        // Capture stdout if this is a test method
        String capturedStdout = null;
        if (testIdentifier.isTest()) {
            // Restore original System.out first
            System.setOut(originalOut);

            // Get captured stdout
            ByteArrayOutputStream baos = stdoutCaptures.remove(id);
            if (baos != null && baos.size() > 0) {
                capturedStdout = baos.toString();
            }
        }

        // 결과 노드 업데이트
        TestResult node = nodeMap.get(id);
        if (node != null) {
            node.setStatus(status);

            Long startTime = startTimeMap.get(id);
            if (startTime != null) {
                node.setDurationMillis(System.currentTimeMillis() - startTime);
            }

            node.setErrorMessage(errorMessage[0]);
            node.setStackTrace(stackTrace[0]);
            node.setStdout(capturedStdout);
        }
    }

    public List<TestResult> findRootResults() {
        return new ArrayList<>(roots);
    }

    public TestSummary buildSummary() {
        TestSummary summary = new TestSummary();
        for (TestResult root : roots) {
            accumulate(summary, root);
        }
        return summary;
    }

    private void accumulate(TestSummary summary, TestResult node) {
        // 자식이 없는 노드(실제 테스트 메서드)만 카운트
        if (node.getChildren().isEmpty()) {
            summary.incTotal();
            summary.addDuration(node.getDurationMillis());

            if (node.getStatus() == TestStatus.SUCCESS) {
                summary.incSuccess();
            } else if (node.getStatus() == TestStatus.FAILED) {
                summary.incFailed();
            } else if (node.getStatus() == TestStatus.SKIPPED) {
                summary.incSkipped();
            }
        }

        for (TestResult child : node.getChildren()) {
            accumulate(summary, child);
        }
    }

    private String getStackTraceAsString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement element : t.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }

    /**
     * TeeOutputStream writes to two output streams simultaneously
     */
    private static class TeeOutputStream extends java.io.OutputStream {
        private final java.io.OutputStream out1;
        private final java.io.OutputStream out2;

        public TeeOutputStream(java.io.OutputStream out1, java.io.OutputStream out2) {
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
