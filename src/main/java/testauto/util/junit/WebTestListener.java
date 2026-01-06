package testauto.util.junit;

import testauto.repository.TestResultMemoryRepository;
import testauto.domain.TestStatus;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebTestListener implements TestExecutionListener {

    private final TestResultMemoryRepository repository;

    // Store original System.out
    private final PrintStream originalOut = System.out;

    // Map to store stdout captures for each test
    private final Map<String, ByteArrayOutputStream> stdoutCaptures = new ConcurrentHashMap<>();

    public WebTestListener(TestResultMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (!testIdentifier.isTest() && !testIdentifier.isContainer()) {
            return;
        }

        String id = testIdentifier.getUniqueId();
        String displayName = testIdentifier.getDisplayName();

        String parentId = testIdentifier.getParentId().orElse(null);

        repository.markStarted(id, displayName, parentId);

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

        repository.markFinished(id, status, errorMessage[0], stackTrace[0], capturedStdout);
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