package testauto.service;

import testauto.domain.TestResult;
import testauto.domain.TestSummary;

import java.util.List;

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
}
