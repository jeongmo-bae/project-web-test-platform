package testauto.repository;

import testauto.domain.TestExecution;
import testauto.domain.TestResultRecord;

import java.util.List;
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
}
