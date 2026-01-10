package testauto.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TestExecution {
    private String executionId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int totalTests;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private long totalDurationMillis;
    private String requesterIp;
    private String classNames;
    private String status; // RUNNING, COMPLETED
}
