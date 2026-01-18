package testauto.repository;

import testauto.domain.TestExecution;
import testauto.domain.TestResultRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TestExecutionRepository {
    void saveExecution(TestExecution execution);
    void updateExecution(TestExecution execution);
    void saveResult(TestResultRecord result);
    void saveAllResults(List<TestResultRecord> results);
    Optional<TestExecution> findExecutionById(String executionId);
    List<TestExecution> findAllExecutions();
    List<TestExecution> findRecentExecutions(int limit);
    List<TestResultRecord> findResultsByExecutionId(String executionId);
    Optional<TestExecution> findLatestExecution();

    // Dashboard statistics
    Map<String, Object> getTodayStats();
    List<Map<String, Object>> getWeeklyTrend();
    List<Map<String, Object>> getRecentFailures(int limit);
    int getTotalTestClasses();

    // Authorization
    boolean isAuthorizedUser(String ip);
}
