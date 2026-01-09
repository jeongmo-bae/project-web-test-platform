package testauto.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TestResultRecord {
    private Long id;
    private String executionId;
    private String testId;
    private String parentTestId;
    private String displayName;
    private TestStatus status;
    private long durationMillis;
    private String errorMessage;
    private String stackTrace;
    private String stdout;
}
