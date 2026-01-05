package testauto.util.junit;

import testauto.repository.TestResultMemoryRepository;
import testauto.domain.TestStatus;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.springframework.stereotype.Component;

@Component
public class WebTestListener implements TestExecutionListener {

    private final TestResultMemoryRepository repository;

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
        repository.markFinished(id, status, errorMessage[0], stackTrace[0]);
    }

    private String getStackTraceAsString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement element : t.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }
}