package testauto.service;

import testauto.domain.TestExecution;
import testauto.domain.TestResult;
import testauto.domain.TestResultRecord;

import java.util.List;
import java.util.Optional;

public interface TestExecutionService {

    /**
     * 테스트 실행 요청 (비동기) - executionId 반환
     */
    String submitTests(List<String> classNames, String requesterIp);

    /**
     * 최근 실행 이력 조회
     */
    List<TestExecution> getRecentExecutions(int limit);

    /**
     * 특정 실행 조회
     */
    Optional<TestExecution> getExecution(String executionId);

    /**
     * 특정 실행의 상세 결과 조회
     */
    List<TestResultRecord> getExecutionResults(String executionId);

    /**
     * 특정 실행의 결과를 트리 형태로 조회
     */
    List<TestResult> getExecutionResultTree(String executionId);
}
