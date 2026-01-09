package testauto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import testauto.repository.TestExecutionRepository;
import testauto.domain.TestExecution;
import testauto.domain.TestResult;
import testauto.domain.TestResultRecord;
import testauto.domain.TestSummary;
import testauto.util.junit.WebTestListener;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestExecutionServiceImpl implements TestExecutionService {

    private final WebTestListener webTestListener;
    private final TestExecutionRepository executionRepository;

    private String currentExecutionId;

    /**
     * 단일 테스트 클래스 실행
     */
    public synchronized void runTests(String className) {
        runTests(List.of(className));
    }

    /**
     * 여러 테스트 클래스 실행
     */
    public synchronized void runTests(List<String> classNames) {
        log.info("Starting test execution for {} classes", classNames.size());
        log.debug("Classes to execute: {}", classNames);

        webTestListener.clear();

        // 실행 시작 기록
        currentExecutionId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now();

        TestExecution execution = TestExecution.builder()
                .executionId(currentExecutionId)
                .startedAt(startedAt)
                .build();

        try {
            executionRepository.saveExecution(execution);
        } catch (Exception e) {
            log.warn("Failed to save execution start to DB: {}", e.getMessage());
        }

        try {
            Launcher launcher = LauncherFactory.create();
            launcher.registerTestExecutionListeners(webTestListener);

            List<DiscoverySelector> selectors = classNames.stream()
                    .<DiscoverySelector>map(className -> {
                        try {
                            return DiscoverySelectors.selectClass(className);
                        } catch (Exception e) {
                            log.warn("Failed to select class: {}", className, e);
                            throw new RuntimeException("Invalid class name: " + className, e);
                        }
                    })
                    .toList();

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectors)
                    .build();

            launcher.execute(request);
            log.info("Test execution completed for {} classes", classNames.size());

            // 실행 완료 후 DB에 결과 저장
            saveResultsToDb();

        } catch (Exception e) {
            log.error("Failed to execute tests for classes: {}", classNames, e);
            throw new RuntimeException("Test execution failed", e);
        }
    }

    /**
     * 테스트 결과를 DB에 저장
     */
    private void saveResultsToDb() {
        try {
            TestSummary summary = webTestListener.buildSummary();
            List<TestResult> results = webTestListener.findRootResults();

            // 실행 정보 업데이트
            TestExecution execution = TestExecution.builder()
                    .executionId(currentExecutionId)
                    .finishedAt(LocalDateTime.now())
                    .totalTests(summary.getTotal())
                    .successCount(summary.getSuccess())
                    .failedCount(summary.getFailed())
                    .skippedCount(summary.getSkipped())
                    .totalDurationMillis(summary.getTotalDurationMillis())
                    .build();

            executionRepository.updateExecution(execution);

            // 결과 상세 저장
            List<TestResultRecord> records = new ArrayList<>();
            for (TestResult result : results) {
                collectResultRecords(result, null, records);
            }
            executionRepository.saveAllResults(records);

            log.info("Saved {} test results to DB for execution {}", records.size(), currentExecutionId);
        } catch (Exception e) {
            log.error("Failed to save test results to DB: {}", e.getMessage(), e);
        }
    }

    /**
     * TestResult 트리를 TestResultRecord 리스트로 변환
     */
    private void collectResultRecords(TestResult result, String parentId, List<TestResultRecord> records) {
        TestResultRecord record = TestResultRecord.builder()
                .executionId(currentExecutionId)
                .testId(result.getId())
                .parentTestId(parentId)
                .displayName(result.getDisplayName())
                .status(result.getStatus())
                .durationMillis(result.getDurationMillis())
                .errorMessage(result.getErrorMessage())
                .stackTrace(result.getStackTrace())
                .stdout(result.getStdout())
                .build();
        records.add(record);

        for (TestResult child : result.getChildren()) {
            collectResultRecords(child, result.getId(), records);
        }
    }

    /**
     * 테스트 실행 요약 정보 조회
     */
    public TestSummary getSummary() {
        return webTestListener.buildSummary();
    }

    /**
     * 테스트 결과 트리 조회
     */
    public List<TestResult> getTestTree() {
        return webTestListener.findRootResults();
    }

    /**
     * 최근 실행 이력 조회
     */
    @Override
    public List<TestExecution> getRecentExecutions(int limit) {
        return executionRepository.findRecentExecutions(limit);
    }

    /**
     * 특정 실행의 상세 결과 조회
     */
    @Override
    public List<TestResultRecord> getExecutionResults(String executionId) {
        return executionRepository.findResultsByExecutionId(executionId);
    }

    /**
     * 특정 실행의 결과를 트리 형태로 조회
     */
    @Override
    public List<TestResult> getExecutionResultTree(String executionId) {
        List<TestResultRecord> records = executionRepository.findResultsByExecutionId(executionId);
        return buildResultTree(records);
    }

    /**
     * TestResultRecord 리스트를 TestResult 트리로 변환
     */
    private List<TestResult> buildResultTree(List<TestResultRecord> records) {
        // testId -> TestResult 맵 생성
        java.util.Map<String, TestResult> resultMap = new java.util.HashMap<>();
        List<TestResult> roots = new ArrayList<>();

        // 모든 레코드를 TestResult로 변환
        for (TestResultRecord record : records) {
            TestResult result = new TestResult(record.getTestId(), record.getDisplayName());
            result.setStatus(record.getStatus());
            result.setDurationMillis(record.getDurationMillis());
            result.setErrorMessage(record.getErrorMessage());
            result.setStackTrace(record.getStackTrace());
            result.setStdout(record.getStdout());
            resultMap.put(record.getTestId(), result);
        }

        // 부모-자식 관계 설정
        for (TestResultRecord record : records) {
            TestResult result = resultMap.get(record.getTestId());
            if (record.getParentTestId() == null) {
                roots.add(result);
            } else {
                TestResult parent = resultMap.get(record.getParentTestId());
                if (parent != null) {
                    parent.addChild(result);
                } else {
                    roots.add(result);
                }
            }
        }

        return roots;
    }
}
