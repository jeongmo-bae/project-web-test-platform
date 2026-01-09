package testauto.service;

import testauto.domain.TestExecution;
import testauto.domain.TestResult;
import testauto.domain.TestResultRecord;
import testauto.domain.TestSummary;

import java.util.List;
import java.util.Optional;

public interface TestExecutionService {

    /**
     * 단일 테스트 클래스 실행
     */
    void runTests(String className);

    /**
     * 여러 테스트 클래스 실행
     */
    void runTests(List<String> classNames);

    /**
     * 테스트 실행 요약 정보 조회
     */
    TestSummary getSummary();

    /**
     * 테스트 결과 트리 조회
     */
    List<TestResult> getTestTree();

    /**
     * 최근 실행 이력 조회
     */
    List<TestExecution> getRecentExecutions(int limit);

    /**
     * 특정 실행의 상세 결과 조회
     */
    List<TestResultRecord> getExecutionResults(String executionId);

    /**
     * 특정 실행의 결과를 트리 형태로 조회
     */
    List<TestResult> getExecutionResultTree(String executionId);
}
